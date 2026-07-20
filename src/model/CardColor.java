package model;

/**
 * Cores possíveis de uma carta do UNO.
 *
 * RED, BLUE, GREEN e YELLOW são usadas em cartas numéricas e de ação.
 * BLACK é exclusiva das cartas coringa (WILD e WILD_DRAW_FOUR).
 */
public enum CardColor {

    /** Cor vermelha. */
    RED,

    /** Cor azul. */
    BLUE,

    /** Cor verde. */
    GREEN,

    /** Cor amarela. */
    YELLOW,

    /**
     * Cor preta — exclusiva dos coringas.
     * Quando jogado, o jogador escolhe uma das quatro cores para a rodada.
     */
    BLACK
}
