package server;

import model.Card;
import model.GameState;
import model.PlayerState;
import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Uma sala representa uma única partida de UNO.
 *
 * A sala roda em sua própria Thread, aguarda ficar cheia e então cria o
 * GameState e uma Thread de ClientHandler para cada jogador.
 */
public final class Room implements Runnable {

    private static final long LOBBY_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private static final class PendingClient {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;
        final String name;
        final int playerId;

        PendingClient(Socket socket, BufferedReader in, PrintWriter out,
                      String name, int playerId) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.name = name;
            this.playerId = playerId;
        }
    }

    private final String code;
    private final int expectedPlayers;
    private final int turnTimeoutSeconds;
    private final int matchDurationMinutes;
    private final RoomManager manager;
    private final List<PendingClient> pendingClients = new ArrayList<>();
    private final List<ClientHandler> handlers = new ArrayList<>();

    private volatile GameState game;
    private volatile boolean started;
    private volatile boolean shuttingDown;
    private volatile boolean finalMessageBroadcast;
    private Integer pendingUnoPlayerId;

    Room(String code, int expectedPlayers, int turnTimeoutSeconds,
         int matchDurationMinutes, RoomManager manager) {
        this.code = code;
        this.expectedPlayers = expectedPlayers;
        this.turnTimeoutSeconds = turnTimeoutSeconds;
        this.matchDurationMinutes = matchDurationMinutes;
        this.manager = manager;
    }

    public String getCode() {
        return code;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    public int getTurnTimeoutSeconds() {
        return turnTimeoutSeconds;
    }

    public synchronized boolean addPlayer(Socket socket, BufferedReader in,
                                          PrintWriter out, String name) {
        if (started || shuttingDown || pendingClients.size() >= expectedPlayers) {
            return false;
        }

        int playerId = pendingClients.size();
        // Registra o jogador na sala enquanto a partida ainda não começou.
        PendingClient client = new PendingClient(socket, in, out, name, playerId);
        pendingClients.add(client);

        out.println(Protocol.buildMessage(Protocol.SALA_JOIN_SUCCESS, code,
                String.valueOf(playerId)));
        broadcastLobbyStatus();

        if (pendingClients.size() == expectedPlayers) {
            notifyAll();
        }
        return true;
    }

    private synchronized void broadcastLobbyStatus() {
        String message = Protocol.buildMessage("AGUARDANDO",
                "Sala " + code + ": " + pendingClients.size() + "/" + expectedPlayers
                        + " jogadores conectados.");
        for (PendingClient client : pendingClients) {
            client.out.println(message);
        }
    }

    @Override
    public void run() {
        ScheduledExecutorService matchTimer = null;
        try {
            synchronized (this) {
                long deadline = System.currentTimeMillis() + LOBBY_TIMEOUT_MILLIS;
                while (!shuttingDown && pendingClients.size() < expectedPlayers) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        shuttingDown = true;
                        for (PendingClient client : pendingClients) {
                            client.out.println(Protocol.buildMessage(
                                    Protocol.FIM_JOGO_PARTIDA_ENCERRADA,
                                    "Sala encerrada por tempo de espera no lobby."));
                        }
                        return;
                    }
                    wait(remaining);
                }
                if (shuttingDown) {
                    return;
                }
                started = true;
            }

            startGame();

            if (matchDurationMinutes > 0) {
                matchTimer = Executors.newSingleThreadScheduledExecutor();
                matchTimer.schedule(this::handleGlobalTimeout,
                        matchDurationMinutes, TimeUnit.MINUTES);
            }

            // Primeiro avisa todos que a partida começou. Só depois as threads
            // de jogador passam a esperar/processar turnos, evitando mensagens de
            // jogada antes do INICIO_PARTIDA.
            broadcast(Protocol.buildMessage(Protocol.MSG_INICIO_PARTIDA,
                    code, joinedPlayerNames()));
            broadcastStateToAll();

            List<Thread> gameThreads = new ArrayList<>();
            for (ClientHandler handler : handlers) {
                Thread thread = new Thread(handler,
                        "ClientHandler-" + code + "-" + handler.getPlayerId());
                gameThreads.add(thread);
                thread.start();
            }

            for (Thread thread : gameThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (matchTimer != null) {
                matchTimer.shutdownNow();
            }
            closeAllClients();
            manager.removeRoom(code, this);
        }
    }

    private void startGame() {
        List<String> names = new ArrayList<>();
        synchronized (this) {
            for (PendingClient client : pendingClients) {
                names.add(client.name);
            }
        }

        game = GameState.create(names);
        List<PlayerState> players = game.getPlayers();

        synchronized (this) {
            handlers.clear();
            for (PendingClient client : pendingClients) {
                ClientHandler handler = new ClientHandler(
                        client.socket,
                        client.playerId,
                        client.name,
                        client.out,
                        client.in,
                        this);
                handler.setGame(game);
                handler.setPlayerState(players.get(client.playerId));
                handlers.add(handler);
            }
        }

        System.out.println("Sala " + code + " iniciou com " + names.size()
                + " jogadores: " + String.join(", ", names));
    }

    private synchronized String joinedPlayerNames() {
        StringJoiner joiner = new StringJoiner(",");
        for (PendingClient client : pendingClients) {
            joiner.add(client.name);
        }
        return joiner.toString();
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler handler : handlers) {
            handler.sendLine(message);
        }
    }

    public void broadcastStateToAll() {
        GameState currentGame = game;
        if (currentGame == null) {
            return;
        }

        Card top = currentGame.getTopCard();
        String topToken = Protocol.formatCard(top);
        String activeColor = currentGame.getActiveColor().toString();
        String currentPlayer = currentGame.currentPlayer().getPlayerName();

        List<ClientHandler> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(handlers);
        }
        for (ClientHandler handler : snapshot) {
            handler.sendLine(Protocol.buildMessage(Protocol.MSG_TOPO, topToken));
            handler.sendLine(Protocol.buildMessage(Protocol.MSG_ATIVA, activeColor));
            handler.sendLine(Protocol.buildMessage(Protocol.MSG_ATUAL, currentPlayer));
            handler.sendStateSnapshot();
        }
    }

    public synchronized void notifyTurnTimeout(ClientHandler handler) {
        if (game == null || game.isGameOver() || shuttingDown) {
            return;
        }

        try {
            List<Card> drawn = game.drawCards(handler.getPlayerId(), 1);
            clearExpiredUnoWindow(handler.getPlayerId());
            handler.sendLine(Protocol.buildMessage(
                    Protocol.MSG_TEMPO_ESGOTADO,
                    "Você demorou e comprou uma carta: " + Protocol.formatHand(drawn)));
            broadcast(Protocol.buildMessage(
                    Protocol.MSG_ATUALIZACAO,
                    handler.getPlayerName() + " estourou o tempo e comprou 1 carta."));
            broadcastStateToAll();
        } catch (IllegalStateException ignored) {
            broadcastStateToAll();
        }
    }

    public synchronized void afterSuccessfulPlay(ClientHandler handler,
                                                 int cardsAfterPlay,
                                                 boolean declaredUno) {
        if (cardsAfterPlay == 1) {
            if (declaredUno) {
                pendingUnoPlayerId = null;
                broadcast(Protocol.buildMessage(
                        Protocol.MSG_ATUALIZACAO,
                        handler.getPlayerName() + " gritou UNO!"));
            } else {
                pendingUnoPlayerId = handler.getPlayerId();
                broadcast(Protocol.buildMessage(
                        Protocol.MSG_ATUALIZACAO,
                        handler.getPlayerName()
                                + " ficou com 1 carta e não declarou UNO. O próximo jogador pode usar DORMIU."));
            }
        } else if (cardsAfterPlay == 0) {
            pendingUnoPlayerId = null;
        }
    }

    public synchronized void handleDormiu(ClientHandler caller) {
        if (pendingUnoPlayerId == null) {
            throw new IllegalStateException("Não há jogador pendente de UNO.");
        }
        if (pendingUnoPlayerId == caller.getPlayerId()) {
            throw new IllegalStateException("Você não pode usar DORMIU contra si mesmo.");
        }

        int punishedId = pendingUnoPlayerId;
        PlayerState punished = game.getPlayers().get(punishedId);
        List<Card> penalty = game.addPenaltyCards(punishedId, 2);
        pendingUnoPlayerId = null;

        broadcast(Protocol.buildMessage(
                Protocol.MSG_ATUALIZACAO,
                caller.getPlayerName() + " chamou DORMIU! " + punished.getPlayerName()
                        + " comprou +2 cartas."));
        ClientHandler punishedHandler = findHandler(punishedId);
        if (punishedHandler != null) {
            punishedHandler.sendLine(Protocol.buildMessage(
                    Protocol.MSG_COMPROU, Protocol.formatHand(penalty)));
        }
        broadcastStateToAll();
    }

    /**
     * A chance de chamar DORMIU termina quando o jogador seguinte realiza uma
     * ação que encerra o turno (JOGAR/COMPRAR/timeout).
     */
    public synchronized void clearExpiredUnoWindow(int actingPlayerId) {
        if (pendingUnoPlayerId != null && pendingUnoPlayerId != actingPlayerId) {
            pendingUnoPlayerId = null;
        }
    }

    public synchronized void notifyPlayerLeft(ClientHandler disconnectedHandler,
                                               String reason) {
        handlers.remove(disconnectedHandler);

        if (reason != null && !reason.isBlank()) {
            System.out.println("[" + code + "] " + reason);
            broadcast(Protocol.buildMessage(Protocol.MSG_ATUALIZACAO, reason));
        }

        if (game == null || game.isGameOver() || shuttingDown) {
            return;
        }

        shuttingDown = true;
        finalMessageBroadcast = true;
        String endMessage = "Partida encerrada: "
                + disconnectedHandler.getPlayerName() + " saiu/desconectou.";

        // Envia o encerramento antes de liberar as threads bloqueadas no turno.
        // Assim, nenhum handler fecha o socket antes de receber a mensagem final.
        broadcast(Protocol.buildMessage(
                Protocol.FIM_JOGO_PARTIDA_ENCERRADA,
                endMessage,
                finalHandsPayload()));
        game.abortGame(endMessage);
        closeAllClients();
    }


    public boolean wasFinalMessageBroadcast() {
        return finalMessageBroadcast;
    }

    public String finalGameMessage() {
        GameState currentGame = game;
        if (currentGame == null) {
            return Protocol.buildMessage(Protocol.FIM_JOGO_PARTIDA_ENCERRADA,
                    "Partida sem estado final.");
        }
        PlayerState winner = currentGame.getWinner();
        if (winner != null) {
            return Protocol.buildMessage(
                    Protocol.FIM_JOGO,
                    "VENCEDOR=" + winner.getPlayerName(),
                    finalHandsPayload());
        }
        return Protocol.buildMessage(
                Protocol.FIM_JOGO_PARTIDA_ENCERRADA,
                currentGame.getEndReason() == null ? "Partida encerrada." : currentGame.getEndReason(),
                finalHandsPayload());
    }

    private String finalHandsPayload() {
        GameState currentGame = game;
        if (currentGame == null) {
            return "MAOS=";
        }
        StringJoiner joiner = new StringJoiner("|");
        for (PlayerState player : currentGame.getPlayers()) {
            joiner.add(player.getPlayerName() + "="
                    + Protocol.formatHand(player.getHandSnapshot()));
        }
        return "MAOS=" + joiner;
    }

    private synchronized ClientHandler findHandler(int playerId) {
        for (ClientHandler handler : handlers) {
            if (handler.getPlayerId() == playerId) {
                return handler;
            }
        }
        return null;
    }

    private void handleGlobalTimeout() {
        synchronized (this) {
            if (game == null || game.isGameOver() || shuttingDown) {
                return;
            }
            shuttingDown = true;
            finalMessageBroadcast = true;
            String reason = "Tempo global da partida esgotado ("
                    + matchDurationMinutes + " min).";

            // Primeiro avisa todos; depois libera/encerra as threads da partida.
            broadcast(Protocol.buildMessage(
                    Protocol.FIM_JOGO_PARTIDA_ENCERRADA,
                    reason,
                    finalHandsPayload()));
            game.abortGame(reason);
            closeAllClients();
        }
    }

    private synchronized void closeAllClients() {
        for (PendingClient client : pendingClients) {
            try {
                client.socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
