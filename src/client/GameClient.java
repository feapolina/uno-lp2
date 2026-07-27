package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static client.AnsiColors.BOLD;
import static client.AnsiColors.BLUE;
import static client.AnsiColors.CYAN;
import static client.AnsiColors.GREEN;
import static client.AnsiColors.MAGENTA;
import static client.AnsiColors.RED;
import static client.AnsiColors.RESET;
import static client.AnsiColors.YELLOW;

public final class GameClient {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java client.GameClient <host> <porta> <nome>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String playerName = args[2];

        printHeader(playerName);

        try (Socket socket = new Socket(host, port);
             BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = socketIn.readLine()) != null) {
                        System.out.println(AnsiFormatter.parse(formatServerMessage(line)));
                        if (line.startsWith("ENTRAR_NOME")) {
                            socketOut.println(playerName);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Conexão encerrada: " + e.getMessage());
                }
            }, "ServerReader");
            readerThread.setDaemon(true);
            readerThread.start();

            String input;
            while ((input = consoleIn.readLine()) != null) {
                if (input.isBlank()) {
                    continue;
                }
                socketOut.println(input.trim());
            }
        } catch (IOException e) {
            System.err.println("Falha ao conectar no servidor: " + e.getMessage());
        }
    }

    private static void printHeader(String playerName) {
        System.out.println(BOLD + BLUE + "========================================" + RESET);
        System.out.println(BOLD + CYAN + "              UNO BR - JOGO UNO" + RESET);
        System.out.println(BOLD + BLUE + "========================================" + RESET);
        System.out.println(YELLOW + "Jogador: " + GREEN + playerName + RESET);
        System.out.println(YELLOW + "Comandos: " + RESET + "JOGAR <COR>:<VALOR> [COR_DECLARADA]  |  COMPRAR  |  SAIR" + RESET);
        System.out.println(BOLD + BLUE + "----------------------------------------" + RESET);
    }

    private static String formatServerMessage(String line) {
        if (line.startsWith("BEM_VINDO")) {
            return "[bold cyan]" + line + "[/bold cyan]";
        }
        if (line.startsWith("INICIO_PARTIDA")) {
            return "[magenta]" + line + "[/magenta]";
        }
        if (line.startsWith("AGUARDANDO")) {
            return "[yellow]" + line + "[/yellow]";
        }
        if (line.startsWith("SUA_VEZ")) {
            return "[green]" + line + "[/green]";
        }
        if (line.startsWith("ATUALIZACAO")) {
            return "[cyan]" + line + "[/cyan]";
        }
        if (line.startsWith("ERRO")) {
            return "[red]" + line + "[/red]";
        }
        if (line.startsWith("TOPO") || line.startsWith("ATIVA") || line.startsWith("ATUAL") || line.startsWith("MAO")) {
            return "[blue]" + line + "[/blue]";
        }
        if (line.startsWith("COMPROU") || line.startsWith("TUDO_BEM") || line.startsWith("FIM_JOGO")) {
            return "[green]" + line + "[/green]";
        }
        return line;
    }
}
