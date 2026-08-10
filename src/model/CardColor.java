package model;

/**
 * Cores possíveis de uma carta do UNO.
 * As cartas normais usam cores reais, enquanto os coringas usam a cor preta.
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
