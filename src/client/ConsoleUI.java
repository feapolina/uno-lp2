package client;

import static client.AnsiColors.*;

/**
 * Responsável por renderizar a interface textual do jogo no terminal.
 * É usada pelo cliente console para exibir o estado da partida de forma organizada.
 */
public class ConsoleUI {

    /**
     * Limpa a tela do terminal antes de redesenhar o quadro do jogo.
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Desenha o quadro principal com status da mesa, mão do jogador e comandos disponíveis.
     */
    public static void renderBoard(String playerName, String currentPlayer, String topCard, String activeColor, String hand, String lastMessage) {
        clearScreen();
        
        System.out.println(BOLD + BLUE + "========================================" + RESET);
        System.out.println(BOLD + CYAN + "              UNO BR - JOGO UNO" + RESET);
        System.out.println(BOLD + BLUE + "========================================" + RESET);
        
        // Bloco de Status
        System.out.println(BOLD + ">>> STATUS DA MESA <<<" + RESET);
        System.out.println("Vez do Jogador : " + (currentPlayer.equals(playerName) ? GREEN + BOLD + "SUA VEZ!" + RESET : YELLOW + currentPlayer + RESET));
        System.out.println("Carta no Topo  : " + formatCardColor(topCard));
        System.out.println("Cor Ativa      : " + formatColorText(activeColor));
        System.out.println(BOLD + BLUE + "----------------------------------------" + RESET);
        
        // Bloco da Mão
        System.out.println(BOLD + ">>> SUA MÃO <<<" + RESET);
        if (hand != null && !hand.isEmpty()) {
            String[] cards = hand.split(",");
            for (int i = 0; i < cards.length; i++) {
                System.out.print(formatCardColor(cards[i]) + "  ");
                if ((i + 1) % 5 == 0) System.out.println(); // Quebra de linha a cada 5 cartas
            }
            if (cards.length % 5 != 0) System.out.println();
        } else {
            System.out.println("Nenhuma carta.");
        }
        System.out.println(BOLD + BLUE + "----------------------------------------" + RESET);
        
        // Bloco de Ajuda Persistente e Mensagens
        System.out.println(BOLD + ">>> COMANDOS <<<" + RESET);
        System.out.println("1. JOGAR <COR>:<VALOR> [COR_DECLARADA] [UNO]");
        System.out.println("2. COMPRAR");
        System.out.println("3. DORMIU   (se o jogador anterior esqueceu UNO)");
        System.out.println("4. SAIR");
        System.out.println(BOLD + BLUE + "----------------------------------------" + RESET);
        
        if (lastMessage != null && !lastMessage.isEmpty()) {
            System.out.println(BOLD + "Último Evento: " + RESET + lastMessage);
            System.out.println(BOLD + BLUE + "----------------------------------------" + RESET);
        }
        
        System.out.print(BOLD + CYAN + "Digite seu comando: " + RESET);
    }

    private static String formatCardColor(String cardToken) {
        if (cardToken == null || cardToken.equals("N/A")) return cardToken;
        if (cardToken.contains("VERMELHO") || cardToken.contains("RED")) return RED + cardToken + RESET;
        if (cardToken.contains("AZUL") || cardToken.contains("BLUE")) return BLUE + cardToken + RESET;
        if (cardToken.contains("VERDE") || cardToken.contains("GREEN")) return GREEN + cardToken + RESET;
        if (cardToken.contains("AMARELO") || cardToken.contains("YELLOW")) return YELLOW + cardToken + RESET;
        if (cardToken.contains("PRETO") || cardToken.contains("BLACK")) return MAGENTA + cardToken + RESET;
        return cardToken;
    }

    private static String formatColorText(String color) {
        return formatCardColor(color);
    }
}