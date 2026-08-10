package server;

import model.Card;
import model.CardColor;
import model.GameState;
import model.PlayerState;
import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Objects;

/**
 * Thread dedicada à comunicação com um jogador durante uma partida.
 * Cada instância representa um jogador conectado e recebe seus comandos de turno.
 */
public final class ClientHandler implements Runnable {

    private enum CommandResult {
        KEEP_TURN,
        END_TURN,
        LEAVE
    }

    private final Socket socket;
    private final int playerId;
    private final String playerName;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Room room;

    private volatile GameState game;
    private volatile PlayerState playerState;

    public ClientHandler(Socket socket, int playerId, String playerName,
                         PrintWriter out, BufferedReader in, Room room) {
        this.socket = Objects.requireNonNull(socket);
        this.playerId = playerId;
        this.playerName = Objects.requireNonNull(playerName);
        this.out = Objects.requireNonNull(out);
        this.in = Objects.requireNonNull(in);
        this.room = Objects.requireNonNull(room);
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setGame(GameState game) {
        this.game = Objects.requireNonNull(game);
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = Objects.requireNonNull(playerState);
    }

    @Override
    public void run() {
        boolean shouldNotifyLeave = false;
        String leaveReason = null;

        try {
            while (!game.isGameOver()) {
                // Espera até que o servidor conceda o turno para este jogador.
                playerState.waitForTurn();
                if (game.isGameOver()) {
                    break;
                }

                // Informa ao cliente que chegou a sua vez e envia o estado atual da partida.
                sendLine(Protocol.buildMessage(Protocol.MSG_SUA_VEZ));
                sendState();

                int timeoutSeconds = room.getTurnTimeoutSeconds();
                long deadlineNanos = timeoutSeconds > 0
                        ? System.nanoTime() + timeoutSeconds * 1_000_000_000L
                        : Long.MAX_VALUE;

                boolean turnFinished = false;
                while (!turnFinished && !game.isGameOver()) {
                    try {
                        configureRemainingTimeout(timeoutSeconds, deadlineNanos);
                        String command = in.readLine();
                        if (command == null) {
                            shouldNotifyLeave = true;
                            leaveReason = playerName + " desconectou.";
                            return;
                        }

                        // Processa o comando do jogador e decide se o turno termina, continua
                        // ou se o jogador sai da partida.
                        CommandResult result = processCommand(command.trim());
                        if (result == CommandResult.LEAVE) {
                            shouldNotifyLeave = true;
                            leaveReason = playerName + " saiu da partida.";
                            return;
                        }
                        if (result == CommandResult.END_TURN) {
                            turnFinished = true;
                        }
                    } catch (SocketTimeoutException timeout) {
                        room.notifyTurnTimeout(this);
                        turnFinished = true;
                    } finally {
                        if (timeoutSeconds > 0 && !socket.isClosed()) {
                            socket.setSoTimeout(0);
                        }
                    }
                }
            }

            if (game.isGameOver() && !socket.isClosed()
                    && !room.wasFinalMessageBroadcast()) {
                sendLine(room.finalGameMessage());
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Erro de comunicação com " + playerName + ": " + e.getMessage());
            }
            shouldNotifyLeave = true;
            leaveReason = playerName + " teve a conexão interrompida.";
        } finally {
            closeQuietly();
            if (shouldNotifyLeave && game != null && !game.isGameOver()) {
                room.notifyPlayerLeft(this, leaveReason);
            }
        }
    }

    private void configureRemainingTimeout(int timeoutSeconds, long deadlineNanos)
            throws SocketTimeoutException, IOException {
        if (timeoutSeconds <= 0) {
            socket.setSoTimeout(0);
            return;
        }

        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new SocketTimeoutException("Tempo do turno esgotado.");
        }
        long remainingMillis = Math.max(1L, remainingNanos / 1_000_000L);
        socket.setSoTimeout((int) Math.min(Integer.MAX_VALUE, remainingMillis));
    }

    private CommandResult processCommand(String commandLine) {
        try {
            String[] tokens = Protocol.parseMessage(commandLine);
            if (tokens.length == 0) {
                throw new IllegalArgumentException("Comando vazio.");
            }

            String command = tokens[0].toUpperCase();

            if (Protocol.CMD_COMPRAR.equals(command)) {
                if (tokens.length != 1) {
                    throw new IllegalArgumentException("Use apenas COMPRAR.");
                }
                List<Card> drawn = game.drawCards(playerId, 1);
                room.clearExpiredUnoWindow(playerId);
                sendLine(Protocol.buildMessage(
                        Protocol.MSG_COMPROU, Protocol.formatHand(drawn)));
                room.broadcast(Protocol.buildMessage(
                        Protocol.MSG_ATUALIZACAO,
                        playerName + " comprou uma carta."));
                sendLine(Protocol.buildMessage(Protocol.MSG_TUDO_BEM));
                room.broadcastStateToAll();
                return CommandResult.END_TURN;
            }

            if (Protocol.CMD_JOGAR.equals(command)) {
                PlayRequest request = parsePlayRequest(tokens);
                int cardsAfterPlay = game.playCard(
                        playerId, request.card, request.declaredColor);

                // Ao realizar a jogada, encerra a janela de DORMIU do jogador anterior.
                room.clearExpiredUnoWindow(playerId);

                room.broadcast(Protocol.buildMessage(
                        Protocol.MSG_ATUALIZACAO,
                        playerName + " jogou " + Protocol.formatCard(request.card) + "."));
                room.afterSuccessfulPlay(this, cardsAfterPlay, request.declaredUno);
                sendLine(Protocol.buildMessage(Protocol.MSG_TUDO_BEM));
                room.broadcastStateToAll();
                return CommandResult.END_TURN;
            }

            if (Protocol.CMD_DORMIU.equals(command)) {
                if (tokens.length != 1) {
                    throw new IllegalArgumentException("Use apenas DORMIU.");
                }
                room.handleDormiu(this);
                sendLine(Protocol.buildMessage(Protocol.MSG_TUDO_BEM));
                return CommandResult.KEEP_TURN;
            }

            if (Protocol.CMD_UNO.equals(command)) {
                throw new IllegalArgumentException(
                        "Declare UNO junto da jogada: JOGAR <carta> [cor] UNO.");
            }

            if (Protocol.CMD_SAIR.equals(command)) {
                sendLine(Protocol.buildMessage(Protocol.MSG_SAINDO, "Até logo!"));
                return CommandResult.LEAVE;
            }

            throw new IllegalArgumentException("Comando desconhecido: " + command);
        } catch (Exception e) {
            sendLine(Protocol.buildMessage(
                    Protocol.MSG_ERRO,
                    e.getMessage() == null ? "Comando inválido." : e.getMessage()));
            room.broadcastStateToAll();
            return CommandResult.KEEP_TURN;
        }
    }

    private PlayRequest parsePlayRequest(String[] tokens) {
        if (tokens.length < 2 || tokens.length > 4) {
            throw new IllegalArgumentException(
                    "Use JOGAR <COR>:<VALOR> [COR_DECLARADA] [UNO].");
        }

        Card card = Protocol.parseCard(tokens[1]);
        CardColor declaredColor = null;
        boolean declaredUno = false;

        if (card.isWild()) {
            if (tokens.length < 3) {
                throw new IllegalArgumentException("Coringa exige uma cor declarada.");
            }
            declaredColor = Protocol.parseColor(tokens[2]);
            if (declaredColor == CardColor.BLACK) {
                throw new IllegalArgumentException("A cor declarada não pode ser PRETO.");
            }
            if (tokens.length == 4) {
                if (!Protocol.CMD_UNO.equalsIgnoreCase(tokens[3])) {
                    throw new IllegalArgumentException("O quarto argumento só pode ser UNO.");
                }
                declaredUno = true;
            }
        } else {
            if (tokens.length == 3) {
                if (!Protocol.CMD_UNO.equalsIgnoreCase(tokens[2])) {
                    throw new IllegalArgumentException(
                            "Carta comum não recebe cor declarada. Use UNO como terceiro argumento, se necessário.");
                }
                declaredUno = true;
            } else if (tokens.length == 4) {
                throw new IllegalArgumentException("Carta comum aceita no máximo o marcador UNO.");
            }
        }

        return new PlayRequest(card, declaredColor, declaredUno);
    }

    private static final class PlayRequest {
        final Card card;
        final CardColor declaredColor;
        final boolean declaredUno;

        PlayRequest(Card card, CardColor declaredColor, boolean declaredUno) {
            this.card = card;
            this.declaredColor = declaredColor;
            this.declaredUno = declaredUno;
        }
    }

    public void sendStateSnapshot() {
        if (playerState != null) {
            sendLine(Protocol.buildMessage(
                    Protocol.MSG_MAO,
                    Protocol.formatHand(playerState.getHandSnapshot())));
        }
    }

    private void sendState() {
        if (game == null || playerState == null) {
            return;
        }
        sendLine(Protocol.buildMessage(
                Protocol.MSG_TOPO, Protocol.formatCard(game.getTopCard())));
        sendLine(Protocol.buildMessage(
                Protocol.MSG_ATIVA, game.getActiveColor().toString()));
        sendLine(Protocol.buildMessage(
                Protocol.MSG_ATUAL, game.currentPlayer().getPlayerName()));
        sendStateSnapshot();
    }

    public synchronized void sendLine(String message) {
        if (!socket.isClosed()) {
            out.println(message);
        }
    }

    public void closeConnection() {
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
