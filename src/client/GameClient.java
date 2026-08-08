package client;

import model.Card;
import model.CardColor;
import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/** Cliente textual simples para o UNO-LP2. */
public final class GameClient {

    private static volatile String currentTop = "N/A";
    private static volatile String activeColor = "N/A";
    private static volatile String currentPlayer = "Aguardando...";
    private static volatile String myHand = "";
    private static volatile String lastEventMessage = "Conectando...";
    private static volatile boolean serverRunning = true;

    private GameClient() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java client.GameClient <host> <porta> <nome>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String playerName = args[2];

        try (BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {
            boolean again = true;
            while (again) {
                resetLocalState();
                iniciarSessao(host, port, playerName, consoleIn);

                System.out.print(AnsiColors.BOLD + AnsiColors.CYAN
                        + "Deseja entrar em outra partida? (S/N): " + AnsiColors.RESET);
                String answer = consoleIn.readLine();
                again = answer != null && answer.trim().equalsIgnoreCase("S");
            }
            System.out.println("Sessão encerrada. Até logo!");
        } catch (IOException e) {
            System.err.println("Erro no console: " + e.getMessage());
        }
    }

    private static void resetLocalState() {
        serverRunning = true;
        currentTop = "N/A";
        activeColor = "N/A";
        currentPlayer = "Aguardando...";
        myHand = "";
        lastEventMessage = "Conectando ao servidor...";
        ConsoleUI.clearScreen();
    }

    private static void iniciarSessao(String host, int port, String playerName,
                                      BufferedReader consoleIn) {
        try (Socket socket = new Socket(host, port);
             BufferedReader socketIn = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true)) {

            if (!performLobbyHandshake(socketIn, socketOut, consoleIn, playerName)) {
                return;
            }

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while (serverRunning && (line = socketIn.readLine()) != null) {
                        processServerMessage(line, playerName);
                    }
                } catch (IOException e) {
                    if (serverRunning) {
                        System.out.println("\nConexão perdida com o servidor.");
                    }
                } finally {
                    serverRunning = false;
                }
            }, "ServerListener");
            readerThread.setDaemon(true);
            readerThread.start();

            ConsoleUI.renderBoard(playerName, currentPlayer, currentTop,
                    activeColor, myHand, lastEventMessage);

            while (serverRunning) {
                if (!consoleIn.ready()) {
                    sleepQuietly(100);
                    continue;
                }

                String input = consoleIn.readLine();
                if (input == null) {
                    break;
                }
                String normalized = input.trim();
                if (normalized.isEmpty()) {
                    continue;
                }

                if (!isValidCommand(normalized)) {
                    lastEventMessage = AnsiColors.RED
                            + "Comando inválido. Consulte a ajuda." + AnsiColors.RESET;
                    ConsoleUI.renderBoard(playerName, currentPlayer, currentTop,
                            activeColor, myHand, lastEventMessage);
                    continue;
                }

                if (!normalized.equalsIgnoreCase("SAIR")
                        && !playerName.equals(currentPlayer)) {
                    lastEventMessage = AnsiColors.YELLOW
                            + "Aguarde sua vez para enviar ações." + AnsiColors.RESET;
                    ConsoleUI.renderBoard(playerName, currentPlayer, currentTop,
                            activeColor, myHand, lastEventMessage);
                    continue;
                }

                socketOut.println(toWireCommand(normalized));
                if (normalized.equalsIgnoreCase("SAIR")) {
                    serverRunning = false;
                    break;
                }
            }

            // Ao sair deste bloco, o try-with-resources fecha o socket e libera
            // a thread leitora que estiver bloqueada em readLine().
        } catch (IOException e) {
            System.out.println("Falha ao conectar no servidor: " + host + ":" + port);
        }
    }

    private static boolean performLobbyHandshake(BufferedReader socketIn,
                                                 PrintWriter socketOut,
                                                 BufferedReader consoleIn,
                                                 String playerName) throws IOException {
        String prompt = socketIn.readLine();
        if (prompt == null) {
            return false;
        }
        System.out.println(formatProtocolLine(prompt));
        System.out.println("Digite: CRIAR <2-8>  ou  ENTRAR <codigo-da-sala>");

        String lobbyCommand;
        while (true) {
            System.out.print("> ");
            lobbyCommand = consoleIn.readLine();
            if (lobbyCommand == null) {
                return false;
            }
            String[] tokens = lobbyCommand.trim().split("\\s+");
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase("CRIAR")) {
                try {
                    int players = Integer.parseInt(tokens[1]);
                    if (players < 2 || players > 8) {
                        throw new NumberFormatException();
                    }
                    socketOut.println(Protocol.buildMessage(
                            Protocol.CMD_CRIAR_SALA, playerName, String.valueOf(players)));
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Quantidade deve estar entre 2 e 8.");
                }
            } else if (tokens.length == 2 && tokens[0].equalsIgnoreCase("ENTRAR")) {
                socketOut.println(Protocol.buildMessage(
                        Protocol.CMD_ENTRAR_SALA, tokens[1].toUpperCase(), playerName));
                break;
            } else {
                System.out.println("Use CRIAR <2-8> ou ENTRAR <codigo>.");
            }
        }

        String line;
        while ((line = socketIn.readLine()) != null) {
            String[] parts = Protocol.parseMessage(line);
            if (parts.length == 0) {
                continue;
            }
            String command = parts[0];
            System.out.println(formatProtocolLine(line));

            if (Protocol.MSG_INICIO_PARTIDA.equals(command)) {
                lastEventMessage = "Partida iniciada.";
                return true;
            }
            if (Protocol.MSG_ERRO.equals(command)) {
                return false;
            }
        }
        return false;
    }

    private static void processServerMessage(String line, String playerName) {
        String[] parts = Protocol.parseMessage(line);
        if (parts.length == 0) {
            return;
        }

        String command = parts[0];
        String payload = parts.length > 1
                ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length))
                : "";
        boolean render = false;

        if (Protocol.MSG_TOPO.equals(command)) {
            currentTop = payload;
        } else if (Protocol.MSG_ATIVA.equals(command)) {
            activeColor = payload;
        } else if (Protocol.MSG_ATUAL.equals(command)) {
            currentPlayer = payload;
        } else if (Protocol.MSG_MAO.equals(command)) {
            myHand = payload;
            render = true;
        } else if (Protocol.MSG_SUA_VEZ.equals(command)) {
            lastEventMessage = "É a sua vez de jogar!";
            render = true;
        } else if (Protocol.FIM_JOGO.equals(command)
                || Protocol.FIM_JOGO_PARTIDA_ENCERRADA.equals(command)) {
            lastEventMessage = "A PARTIDA ACABOU! " + payload;
            ConsoleUI.renderBoard(playerName, currentPlayer, currentTop,
                    activeColor, myHand, lastEventMessage);
            serverRunning = false;
            return;
        } else {
            lastEventMessage = formatServerMessage(command, payload);
            render = true;
        }

        if (render && serverRunning) {
            ConsoleUI.renderBoard(playerName, currentPlayer, currentTop,
                    activeColor, myHand, lastEventMessage);
        }
    }

    private static boolean isValidCommand(String commandLine) {
        if (commandLine.equalsIgnoreCase("COMPRAR")
                || commandLine.equalsIgnoreCase("DORMIU")
                || commandLine.equalsIgnoreCase("SAIR")) {
            return true;
        }
        if (!commandLine.toUpperCase().startsWith("JOGAR ")) {
            return false;
        }

        String[] tokens = commandLine.split("\\s+");
        if (tokens.length < 2 || tokens.length > 4) {
            return false;
        }

        try {
            Card card = Protocol.parseCard(tokens[1]);
            if (card.isWild()) {
                if (tokens.length < 3) {
                    return false;
                }
                CardColor declared = Protocol.parseColor(tokens[2]);
                if (declared == CardColor.BLACK) {
                    return false;
                }
                return tokens.length == 3
                        || (tokens.length == 4
                            && tokens[3].equalsIgnoreCase(Protocol.CMD_UNO));
            }

            return tokens.length == 2
                    || (tokens.length == 3
                        && tokens[2].equalsIgnoreCase(Protocol.CMD_UNO));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String toWireCommand(String userCommand) {
        String[] tokens = userCommand.trim().split("\\s+");
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
        return Protocol.buildMessage(tokens[0].toUpperCase(), args);
    }

    private static String formatServerMessage(String command, String payload) {
        String text = payload == null || payload.isBlank()
                ? command
                : command + " " + payload;
        if (Protocol.MSG_ERRO.equals(command)) {
            return AnsiColors.RED + text + AnsiColors.RESET;
        }
        if (Protocol.MSG_TEMPO_ESGOTADO.equals(command)) {
            return AnsiColors.YELLOW + text + AnsiColors.RESET;
        }
        return text;
    }

    private static String formatProtocolLine(String line) {
        String[] parts = Protocol.parseMessage(line);
        return String.join(" ", parts);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
