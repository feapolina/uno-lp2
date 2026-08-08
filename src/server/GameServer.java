package server;

import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Servidor central multithreaded.
 *
 * Cada conexão recebe uma Thread curta de lobby. Depois do handshake, o socket
 * é entregue a uma Room, que gerencia a partida e cria uma Thread de
 * ClientHandler para cada jogador.
 */
public final class GameServer {

    public static final int DEFAULT_TURN_TIMEOUT_SECONDS = 15;
    public static final int DEFAULT_MATCH_DURATION_MINUTES = 15;

    private final int port;
    private final RoomManager roomManager;

    public GameServer(int port) {
        this(port, DEFAULT_TURN_TIMEOUT_SECONDS, DEFAULT_MATCH_DURATION_MINUTES);
    }

    public GameServer(int port, int turnTimeoutSeconds, int matchDurationMinutes) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Porta inválida: " + port);
        }
        if (turnTimeoutSeconds < 0) {
            throw new IllegalArgumentException("Timeout de turno não pode ser negativo.");
        }
        if (matchDurationMinutes < 0) {
            throw new IllegalArgumentException("Duração da partida não pode ser negativa.");
        }
        this.port = port;
        this.roomManager = new RoomManager(turnTimeoutSeconds, matchDurationMinutes);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 3) {
            System.err.println("Uso: java server.GameServer <porta> [timeout-turno-segundos] [duracao-partida-minutos]");
            System.err.println("Padrão: turno=15s, partida=15min. Use 0 para desativar um timer.");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int turnTimeout = args.length >= 2
                ? Integer.parseInt(args[1])
                : DEFAULT_TURN_TIMEOUT_SECONDS;
        int matchDuration = args.length == 3
                ? Integer.parseInt(args[2])
                : DEFAULT_MATCH_DURATION_MINUTES;

        new GameServer(port, turnTimeout, matchDuration).start();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor UNO iniciado na porta " + port + ".");
            System.out.println("Até " + RoomManager.MAX_ROOMS + " salas simultâneas.");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleLobby(socket),
                        "Lobby-" + socket.getRemoteSocketAddress()).start();
            }
        }
    }

    private void handleLobby(Socket socket) {
        boolean handedToRoom = false;
        try {
            socket.setSoTimeout(30_000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(Protocol.buildMessage(
                    Protocol.MSG_LOBBY,
                    "Use CRIAR_SALA;<nome>;<2-8> ou ENTRAR_SALA;<codigo>;<nome>"));

            String raw;
            try {
                raw = in.readLine();
            } catch (SocketTimeoutException e) {
                out.println(Protocol.buildMessage(Protocol.MSG_ERRO,
                        "Tempo esgotado no lobby."));
                return;
            }

            String[] parts = Protocol.parseMessage(raw);
            if (parts.length == 0) {
                out.println(Protocol.buildMessage(Protocol.MSG_ERRO,
                        "Comando de lobby vazio."));
                return;
            }

            String command = parts[0].toUpperCase();
            Room room;
            String playerName;

            if (Protocol.CMD_CRIAR_SALA.equals(command)) {
                if (parts.length != 3) {
                    throw new IllegalArgumentException(
                            "Use CRIAR_SALA;<nome>;<numero-jogadores>.");
                }
                playerName = normalizedName(parts[1]);
                int expectedPlayers = Integer.parseInt(parts[2]);
                room = roomManager.createRoom(expectedPlayers);
                out.println(Protocol.buildMessage(
                        Protocol.MSG_SALA_CRIADA, room.getCode()));
            } else if (Protocol.CMD_ENTRAR_SALA.equals(command)) {
                if (parts.length != 3) {
                    throw new IllegalArgumentException(
                            "Use ENTRAR_SALA;<codigo>;<nome>.");
                }
                String code = parts[1].trim().toUpperCase();
                playerName = normalizedName(parts[2]);
                room = roomManager.getRoom(code);
                if (room == null) {
                    throw new IllegalArgumentException("Sala não encontrada: " + code);
                }
            } else {
                throw new IllegalArgumentException("Comando de lobby desconhecido: " + command);
            }

            socket.setSoTimeout(0);
            if (!room.addPlayer(socket, in, out, playerName)) {
                throw new IllegalStateException("A sala já iniciou ou está cheia.");
            }
            handedToRoom = true;
            System.out.println(playerName + " entrou em " + room.getCode()
                    + " (" + room.getExpectedPlayers() + " jogadores). Salas ativas: "
                    + roomManager.activeRoomCount());
        } catch (Exception e) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(Protocol.buildMessage(Protocol.MSG_ERRO,
                        e.getMessage() == null ? "Falha no lobby." : e.getMessage()));
            } catch (IOException ignored) {
            }
        } finally {
            if (!handedToRoom) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String normalizedName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Nome do jogador não pode ser vazio.");
        }
        if (name.contains(";") || name.contains("|") || name.contains("=")) {
            throw new IllegalArgumentException("Nome contém caractere reservado (; | =). ");
        }
        return name;
    }
}
