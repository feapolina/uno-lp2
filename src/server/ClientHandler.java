package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Objects;
import model.Card;
import model.CardColor;
import model.GameState;
import model.PlayerState;
import shared.Protocol;

public final class ClientHandler implements Runnable {

    private final Socket socket;
    private final int playerId;
    private final String playerName;
    private final PrintWriter out;
    private final BufferedReader in;
    private final GameServer server;

    private volatile GameState game;
    private volatile PlayerState playerState;

    public ClientHandler(Socket socket, int playerId, String playerName,
                         PrintWriter out, BufferedReader in, GameServer server) {
        this.socket = Objects.requireNonNull(socket);
        this.playerId = playerId;
        this.playerName = Objects.requireNonNull(playerName);
        this.out = Objects.requireNonNull(out);
        this.in = Objects.requireNonNull(in);
        this.server = Objects.requireNonNull(server);
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
            out.println("BEM_VINDO " + playerId);
            sendLine("NOME_JOGADOR " + playerName);
            sendState();

            while (!game.isGameOver()) {
                playerState.waitForTurn();
                if (game.isGameOver()) {
                    break;
                }
                sendLine("SUA_VEZ");
                sendState();

                String command;
                try {
                    socket.setSoTimeout(GameServer.TURN_TIMEOUT_SECONDS * 1000);
                    command = in.readLine();
                } catch (SocketTimeoutException timeout) {
                    server.notifyTurnTimeout(this);
                    continue;
                } finally {
                    socket.setSoTimeout(0);
                }

                if (command == null) {
                    shouldNotifyLeave = true;
                    leaveReason = playerName + " desconectou.";
                    break;
                }
                boolean keepPlaying = processCommand(command.trim());
                if (!keepPlaying) {
                    shouldNotifyLeave = true;
                    leaveReason = playerName + " saiu da partida.";
                    break;
                }
            }
            if (game.isGameOver()) {
                if (game.getWinner() != null) {
                    sendLine("FIM_JOGO " + game.getWinner().getPlayerName());
                } else {
                    sendLine("FIM_JOGO_PARTIDA_ENCERRADA " + game.getEndReason());
                }
            }
        } catch (IOException e) {
            if (!"Socket closed".equalsIgnoreCase(e.getMessage())) {
                System.err.println("Erro de comunicação com jogador " + playerName + ": " + e.getMessage());
            }
            shouldNotifyLeave = true;
            leaveReason = playerName + " teve a conexão interrompida.";
        } finally {
            closeQuietly();
            if (shouldNotifyLeave && game != null && !game.isGameOver()) {
                server.notifyPlayerLeft(this, leaveReason);
            }
        }
    }

    private boolean processCommand(String commandLine) {
        try {
            if (commandLine.equalsIgnoreCase("COMPRAR")) {
                List<Card> drawn = game.drawCards(playerId, 1);
                sendLine("COMPROU " + Protocol.formatHand(drawn));
                server.broadcast("ATUALIZACAO " + playerName + " comprou uma carta.");
            } else if (commandLine.toUpperCase().startsWith("JOGAR ")) {
                String[] tokens = commandLine.split(" ");
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Comando JOGAR inválido. Use JOGAR <COR>:<VALOR> [COR_DECLARADA].");
                }
                Card card = Protocol.parseCard(tokens[1]);
                CardColor declaredColor = null;
                if (tokens.length == 3) {
                    declaredColor = Protocol.parseColor(tokens[2]);
                }
                // FIX #4 — cardsAfterPlay capturado dentro do lock do GameState (retorno
                // de playCard), tornando a detecção de UNO atômica e livre de race condition
                // com o timeout que poderia adicionar uma carta entre a jogada e o check.
                int cardsAfterPlay = game.playCard(playerId, card, declaredColor);
                server.broadcast("ATUALIZACAO " + playerName + " jogou " + Protocol.formatCard(card) + ".");
                if (cardsAfterPlay == 1) {
                    server.broadcast("ATUALIZACAO " + playerName + " gritou UNO!");
                }
            } else if (commandLine.equalsIgnoreCase("SAIR")) {
                sendLine("SAINDO Até logo!");
                return false;
            } else {
                throw new IllegalArgumentException("Comando desconhecido: " + commandLine);
            }
            sendLine("TUDO_BEM");
            server.broadcastStateToAll();
            return true;
        } catch (Exception e) {
            sendLine("ERRO " + e.getMessage());
            server.broadcastStateToAll();
            return true;
        }
    }

    public void sendStateSnapshot() {
        if (playerState == null) {
            return;
        }
        sendLine("MAO " + Protocol.formatHand(playerState.getHandSnapshot()));
    }

    private void sendState() {
        if (game == null || playerState == null) {
            return;
        }
        sendLine("TOPO " + Protocol.formatCard(game.getTopCard()));
        sendLine("ATIVA " + game.getActiveColor());
        sendLine("ATUAL " + game.currentPlayer().getPlayerName());
        sendLine("MAO " + Protocol.formatHand(playerState.getHandSnapshot()));
    }

    public void sendLine(String message) {
        out.println(message);
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
