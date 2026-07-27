package client;

import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static client.AnsiColors.BOLD;
import static client.AnsiColors.BLUE;
import static client.AnsiColors.CYAN;
import static client.AnsiColors.GREEN;
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

            socketOut.println(playerName);

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = socketIn.readLine()) != null) {
                        System.out.println(AnsiFormatter.parse(formatServerMessage(line)));
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

                String normalized = input.trim();
                if (!isValidCommand(normalized)) {
                    System.out.println(AnsiFormatter.parse("[red]Comando inválido. Use COMPRAR, SAIR ou JOGAR <COR>:<VALOR> [COR_DECLARADA].[/red]"));
                    continue;
                }

                socketOut.println(normalized);
                if (normalized.equalsIgnoreCase("SAIR")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Falha ao conectar no servidor: " + e.getMessage());
        }
    }

    private static boolean isValidCommand(String commandLine) {
        if (commandLine.equalsIgnoreCase("COMPRAR") || commandLine.equalsIgnoreCase("SAIR")) {
            return true;
        }

        if (!commandLine.toUpperCase().startsWith("JOGAR ")) {
            return false;
        }

        String[] tokens = commandLine.split("\\s+");
        if (tokens.length < 2 || tokens.length > 3) {
            return false;
        }

        try {
            Protocol.parseCard(tokens[1]);
            if (tokens.length == 3) {
                Protocol.parseColor(tokens[2]);
            }
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
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
        if (line.startsWith("SAINDO")) {
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
