package model;

/**
 * Valores (tipos) possíveis de uma carta do UNO.
 *
 * Divisão:
 * - Numéricas (ZERO–NINE): cartas comuns sem efeito especial.
 * - Ação colorida (SKIP, REVERSE, DRAW_TWO): afetam o fluxo do jogo.
 * - Coringa (WILD, WILD_DRAW_FOUR): sem cor (BLACK), jogáveis sempre.
 */
public enum CardValue {

    // Numéricas

    /** Zero — 1 por cor no baralho padrão. */
    ZERO,
    /** Um — 2 por cor. */
    ONE,
    /** Dois — 2 por cor. */
    TWO,
    /** Três — 2 por cor. */
    THREE,
    /** Quatro — 2 por cor. */
    FOUR,
    /** Cinco — 2 por cor. */
    FIVE,
    /** Seis — 2 por cor. */
    SIX,
    /** Sete — 2 por cor. */
    SEVEN,
    /** Oito — 2 por cor. */
    EIGHT,
    /** Nove — 2 por cor. */
    NINE,

    // Ação (coloridas)

    /** O próximo jogador perde a vez. 2 por cor. */
    SKIP,

    /**
     * Inverte o sentido do jogo. 2 por cor.
     * Em partidas de 2 jogadores age como SKIP.
     */
    REVERSE,

    /** O próximo jogador compra 2 cartas e perde a vez. 2 por cor. */
    DRAW_TWO,

    // Coringa (BLACK)

    /** Pode ser jogado sempre; o jogador escolhe a nova cor. 4 no baralho. */
    WILD,

    /**
     * O próximo jogador compra 4 cartas e perde a vez; o jogador escolhe a cor.
     * Só é legal quando não há carta da cor ativa na mão. 4 no baralho.
     */
    WILD_DRAW_FOUR
}
