package server;

import model.GameState;
import model.PlayerState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GameServer {

    private final int port;
    private final int expectedPlayers;
    private final List<ClientHandler> handlers;
    private final List<String> playerNames;

    public GameServer(int port, int expectedPlayers) {
        if (expectedPlayers < 2) {
            throw new IllegalArgumentException("É necessário no mínimo 2 jogadores.");
        }
        this.port = port;
        this.expectedPlayers = expectedPlayers;
        this.handlers = new ArrayList<>(expectedPlayers);
        this.playerNames = new ArrayList<>(expectedPlayers);
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

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port + ". Aguardando " + expectedPlayers + " jogadores...");
            while (playerNames.size() < expectedPlayers) {
                Socket socket = serverSocket.accept();
                registerPlayer(socket);
            }

            GameState game = GameState.create(playerNames, new Random());
            System.out.println("Todos os jogadores conectados. Iniciando partida...");

            for (ClientHandler handler : handlers) {
                PlayerState playerState = game.getPlayers().stream()
                        .filter(player -> player.getPlayerId() == handler.getPlayerId())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Jogador não encontrado: " + handler.getPlayerId()));
                handler.setGame(game);
                handler.setPlayerState(playerState);
                new Thread(handler, "ClientHandler-" + handler.getPlayerId()).start();
            }

            broadcast("INICIO_PARTIDA " + String.join(",", playerNames));
            broadcast("TOPO " + game.getTopCard().getColor() + ":" + game.getTopCard().getValue());
        }
    }

    private void registerPlayer(Socket socket) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("ENTRAR_NOME");
            String name = in.readLine();
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

    public synchronized void notifyPlayerLeft(ClientHandler disconnectedHandler, String reason) {
        handlers.remove(disconnectedHandler);
        if (reason != null && !reason.isBlank()) {
            System.out.println(reason);
            broadcast("ATUALIZACAO " + reason);
        }

        if (handlers.size() < 2) {
            broadcast("FIM_JOGO_PARTIDA_ENCERRADA Jogadores insuficientes para continuar.");
        }
    }
}
