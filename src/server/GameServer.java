package server;

import model.Card;
import model.CardColor;
import model.GameState;
import model.PlayerState;
import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GameServer {

    public static final int TURN_TIMEOUT_SECONDS = 10;

    private final int port;
    private final int expectedPlayers;
    private final List<ClientHandler> handlers;
    private final List<String> playerNames;

    private volatile GameState game;
    private volatile boolean shuttingDown;

    public GameServer(int port, int expectedPlayers) {
        if (expectedPlayers < 2) {
            throw new IllegalArgumentException("É necessário no mínimo 2 jogadores.");
        }
        this.port = port;
        this.expectedPlayers = expectedPlayers;
        this.handlers = new ArrayList<>(expectedPlayers);
        this.playerNames = new ArrayList<>(expectedPlayers);
        this.shuttingDown = false;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: java server.GameServer <porta> <numero-de-jogadores>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int players = Integer.parseInt(args[1]);
        new GameServer(port, players).start();
    }

    /**
     * Inicia o servidor e mantém o ServerSocket aberto para múltiplas partidas.
     *
     * Cada chamada a runOneGame() aguarda o fim de uma partida completa (join em
     * todas as threads de handler) antes de reiniciar o ciclo de registro. Isso
     * permite que o cliente use o prompt "Deseja reconectar? (S/N)" para entrar
     * em uma nova sessão sem precisar reiniciar o servidor.
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port
                    + ". Pressione Ctrl+C para encerrar.");
            while (true) {
                runOneGame(serverSocket);
                System.out.println("\n=== Partida encerrada. Aguardando nova sessão... ===\n");
            }
        }
    }

    /**
     * Executa uma partida completa:
     * 1. Aguarda o número esperado de jogadores se registrarem.
     * 2. Cria o GameState e lança as threads de ClientHandler.
     * 3. Bloqueia até todas as threads de handler finalizarem (join).
     *
     * Ao retornar, o estado (handlers, playerNames, game) é resetado de forma
     * sincronizada para que o próximo ciclo comece limpo.
     */
    private void runOneGame(ServerSocket serverSocket) throws IOException {
        // Reinicia o estado para a nova partida
        synchronized (this) {
            handlers.clear();
            playerNames.clear();
            game = null;
            shuttingDown = false;
        }

        System.out.println("Aguardando " + expectedPlayers + " jogadores...");
        while (playerNames.size() < expectedPlayers) {
            Socket socket = serverSocket.accept();
            registerPlayer(socket);
        }

        game = GameState.create(playerNames, new Random());
        System.out.println("Todos os jogadores conectados. Iniciando partida...");

        // Lança as threads e guarda referências para fazer join ao final
        List<Thread> gameThreads = new ArrayList<>(handlers.size());
        for (ClientHandler handler : handlers) {
            PlayerState playerState = game.getPlayers().stream()
                    .filter(player -> player.getPlayerId() == handler.getPlayerId())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Jogador não encontrado: " + handler.getPlayerId()));
            handler.setGame(game);
            handler.setPlayerState(playerState);
            Thread t = new Thread(handler, "ClientHandler-" + handler.getPlayerId());
            gameThreads.add(t);
            t.start();
        }

        broadcast("INICIO_PARTIDA " + String.join(",", playerNames));
        broadcastStateToAll();

        // Aguarda todas as threads de handler finalizarem antes de resetar o estado.
        // Isso garante que não há acesso concorrente durante o reset.
        for (Thread t : gameThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


    private void registerPlayer(Socket socket) {
        try {
            // FIX #8 — timeout de 5 s para o envio do nome; evita que um cliente
            // que nunca envia dados bloqueie o servidor indefinidamente.
            socket.setSoTimeout(5_000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("ENTRAR_NOME");
            String name;
            try {
                name = in.readLine();
            } catch (SocketTimeoutException e) {
                name = null; // usa nome padrão
            }
            socket.setSoTimeout(0); // remove o timeout após o registro
            if (name == null || name.isBlank()) {
                name = "Jogador" + playerNames.size();
            }
            int id = playerNames.size();
            playerNames.add(name);
            ClientHandler handler = new ClientHandler(socket, id, name, out, in, this);
            handlers.add(handler);
            out.println("AGUARDANDO " + (expectedPlayers - playerNames.size()) + " jogadores restantes...");
            System.out.println("Jogador conectado: " + name + " (id=" + id + ")");
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao registrar jogador.", e);
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler handler : handlers) {
            handler.sendLine(message);
        }
    }

    public void broadcastStateToAll() {
        if (game == null) {
            return;
        }

        // FIX #6 — lê o estado do GameState FORA do lock do servidor para evitar
        // deadlock por inversão de ordem de aquisição (GameState.lock ↔ GameServer.this).
        // Cenário eliminado: ClientHandler-A segura GameState.lock e tenta GameServer.this;
        // ClientHandler-B segura GameServer.this e tenta GameState.lock.
        Card topCard = game.getTopCard();
        String formattedTop = Protocol.formatCard(topCard);
        String activeColor = game.getActiveColor().toString();
        String currentName = game.currentPlayer().getPlayerName();

        // Sincroniza apenas para iterar sobre a lista de handlers de forma segura.
        synchronized (this) {
            for (ClientHandler handler : handlers) {
                handler.sendLine("TOPO " + formattedTop);
                handler.sendLine("ATIVA " + activeColor);
                handler.sendLine("ATUAL " + currentName);
                handler.sendStateSnapshot();
            }
        }
    }

    public synchronized void notifyTurnTimeout(ClientHandler handler) {
        if (game == null || game.isGameOver() || shuttingDown) {
            return;
        }

        try {
            List<Card> drawn = game.drawCards(handler.getPlayerId(), 1);
            handler.sendLine("TEMPO_ESGOTADO Você demorou e comprou uma carta: " + Protocol.formatHand(drawn));
            broadcast("ATUALIZACAO " + handler.getPlayerName() + " estourou o tempo e comprou 1 carta.");
            broadcastStateToAll();
        } catch (IllegalStateException ex) {
            // Se a vez já mudou por outro evento, apenas sincroniza estado.
            broadcastStateToAll();
        }
    }

    public synchronized void notifyPlayerLeft(ClientHandler disconnectedHandler, String reason) {
        handlers.remove(disconnectedHandler);

        if (reason != null && !reason.isBlank()) {
            System.out.println(reason);
            broadcast("ATUALIZACAO " + reason);
        }

        if (game == null || game.isGameOver()) {
            return;
        }

        if (handlers.size() < 2) {
            shuttingDown = true;
            String endMessage = "Partida encerrada: jogadores insuficientes para continuar.";
            game.abortGame(endMessage);
            broadcast("FIM_JOGO_PARTIDA_ENCERRADA " + endMessage);
            closeAllClients();
            return;
        }

        broadcastStateToAll();
    }

    private synchronized void closeAllClients() {
        for (ClientHandler handler : new ArrayList<>(handlers)) {
            handler.closeConnection();
        }
    }
}
