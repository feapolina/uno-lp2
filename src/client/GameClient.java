package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import shared.Protocol;

public final class GameClient {
    
    // Variáveis de estado local para renderizar a UI
    private static volatile String currentTop = "N/A";
    private static volatile String activeColor = "N/A";
    private static volatile String currentPlayer = "Aguardando...";
    private static volatile String myHand = "";
    private static volatile String lastEventMessage = "Conectado ao servidor.";
    
    // Flag para controlar se a thread de leitura do servidor ainda deve rodar
    private static volatile boolean serverRunning = true;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java client.GameClient <host> <porta> <nome>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String playerName = args[2];

        try (BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {
            boolean jogarNovamente = true;

            while (jogarNovamente) {
                // Reseta o estado completo para uma nova partida
                serverRunning = true;
                currentTop     = "N/A";
                activeColor    = "N/A";
                currentPlayer  = "Aguardando...";
                myHand         = "";
                lastEventMessage = "Conectando ao servidor...";
                ConsoleUI.clearScreen();

                iniciarSessao(host, port, playerName, consoleIn);

                // Quando a sessão acaba, pergunta se quer reconectar
                System.out.println();
                System.out.print(AnsiColors.BOLD + AnsiColors.CYAN + "Deseja conectar em uma nova partida? (S/N): " + AnsiColors.RESET);
                String resposta = consoleIn.readLine();
                
                if (resposta == null || !resposta.trim().equalsIgnoreCase("S")) {
                    jogarNovamente = false;
                    System.out.println(AnsiColors.BOLD + AnsiColors.YELLOW + "\nSessão encerrada. Até logo!" + AnsiColors.RESET);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler entrada do console: " + e.getMessage());
        }
    }

    private static void iniciarSessao(String host, int port, String playerName, BufferedReader consoleIn) {
        try (Socket socket = new Socket(host, port);
             BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true)) {

            socketOut.println(playerName);

            // Thread para ouvir o servidor
            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while (serverRunning && (line = socketIn.readLine()) != null) {
                        processServerMessage(line, playerName);
                    }
                } catch (IOException e) {
                    // Só reporta erro se o fechamento NÃO foi intencional (evita falso-positivo)
                    if (serverRunning) {
                        System.out.println(AnsiColors.RED + "\nConexão perdida com o servidor." + AnsiColors.RESET);
                        serverRunning = false;
                    }
                }
            }, "ServerReader");
            
            readerThread.setDaemon(true);
            readerThread.start();

            // Loop de envio de comandos
            String input;
            while (serverRunning && (input = consoleIn.readLine()) != null) {
                if (input.isBlank()) continue;

                String normalized = input.trim();
                
                if (!isValidCommand(normalized)) {
                    lastEventMessage = AnsiColors.RED + "Comando inválido. Tente novamente." + AnsiColors.RESET;
                    ConsoleUI.renderBoard(playerName, currentPlayer, currentTop, activeColor, myHand, lastEventMessage);
                    continue;
                }

                socketOut.println(normalized);
                
                if (normalized.equalsIgnoreCase("SAIR")) {
                    System.out.println("\n" + AnsiColors.BOLD + AnsiColors.YELLOW + "Você saiu da partida." + AnsiColors.RESET + "\n");
                    serverRunning = false;
                    // FIX #9 — fecha o socket imediatamente para desbloquear a ServerReader
                    // que está parada em readLine(). Sem isso, a thread só encerraria quando
                    // o try-with-resources fechasse o socket (após join de 2 s), abrindo uma
                    // janela onde a ServerReader da sessão antiga ainda vive durante a reconexão.
                    try { socket.close(); } catch (IOException ignored) { }
                    break;
                }
            }

            // Aguarda a thread leitora encerrar de forma limpa (evita race condition
            // onde ela imprime "Conexão perdida" após o socket ser fechado intencionalmente).
            try {
                readerThread.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            System.out.println(AnsiColors.RED + "\nFalha ao conectar no servidor: " + host + ":" + port + AnsiColors.RESET);
        }
    }

    private static void processServerMessage(String line, String playerName) {
        boolean shouldRender = false;

        if (line.startsWith("TOPO ")) {
            currentTop = line.substring(5);
        } else if (line.startsWith("ATIVA ")) {
            activeColor = line.substring(6);
        } else if (line.startsWith("ATUAL ")) {
            currentPlayer = line.substring(6);
        } else if (line.startsWith("MAO ")) {
            myHand = line.substring(4);
            shouldRender = true; 
        } else if (line.startsWith("SUA_VEZ")) {
            lastEventMessage = AnsiColors.GREEN + AnsiColors.BOLD + "É a sua vez de jogar!" + AnsiColors.RESET;
            shouldRender = true; // precisa atualizar o tabuleiro para mostrar que é a vez do jogador
        } else if (line.startsWith("ATUALIZACAO ") || line.startsWith("ERRO ") || line.startsWith("TEMPO_ESGOTADO ")) {
            lastEventMessage = AnsiFormatter.parse(formatServerMessage(line));
            shouldRender = true; // qualquer evento de jogo deve re-renderizar o tabuleiro
        } else if (line.startsWith("FIM_JOGO")) {
            lastEventMessage = AnsiColors.BOLD + AnsiColors.MAGENTA + "A PARTIDA ACABOU! " + line + AnsiColors.RESET;
            // Renderiza ANTES de setar serverRunning = false; do contrário a guarda abaixo bloquearia.
            ConsoleUI.renderBoard(playerName, currentPlayer, currentTop, activeColor, myHand, lastEventMessage);
            serverRunning = false; // libera o loop principal para perguntar sobre reconexão
            return; // não precisa passar pela guarda de renderização abaixo
        } else {
            lastEventMessage = AnsiFormatter.parse(formatServerMessage(line));
            shouldRender = true;
        }

        if (shouldRender && serverRunning) {
            ConsoleUI.renderBoard(playerName, currentPlayer, currentTop, activeColor, myHand, lastEventMessage);
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
        if (tokens.length < 2 || tokens.length > 3) return false;
        
        try {
            model.Card card = Protocol.parseCard(tokens[1]);
            // Cartas Wild e Wild+4 obrigam a declarar a cor escolhida
            boolean isWild = card.getValue() == model.CardValue.WILD
                          || card.getValue() == model.CardValue.WILD_DRAW_FOUR;
            if (isWild && tokens.length != 3) {
                return false; // Wild sem cor declarada é inválido localmente
            }
            if (tokens.length == 3) {
                Protocol.parseColor(tokens[2]);
            }
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String formatServerMessage(String line) {
        if (line.startsWith("BEM_VINDO")) return "[bold cyan]" + line + "[/bold cyan]";
        if (line.startsWith("INICIO_PARTIDA")) return "[magenta]" + line + "[/magenta]";
        if (line.startsWith("AGUARDANDO")) return "[yellow]" + line + "[/yellow]";
        if (line.startsWith("SAINDO")) return "[yellow]" + line + "[/yellow]";
        if (line.startsWith("TEMPO_ESGOTADO")) return "[yellow]" + line + "[/yellow]";
        if (line.startsWith("ATUALIZACAO")) return "[cyan]" + line + "[/cyan]";
        if (line.startsWith("ERRO")) return "[red]" + line + "[/red]";
        if (line.startsWith("COMPROU") || line.startsWith("TUDO_BEM") || line.startsWith("FIM_JOGO")) return "[green]" + line + "[/green]";
        return line;
    }
}