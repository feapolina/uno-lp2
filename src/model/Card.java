package model;

/**
 * Representa uma carta do UNO.
 *
 * É imutável: uma vez criada, cor e valor nunca mudam.
 * Isso torna a classe segura para uso em ambientes multithreaded.
 *
 * Regras de combinação:
 * - Cartas numéricas e de ação devem ter cor RED, BLUE, GREEN ou YELLOW.
 * - Coringas (WILD, WILD_DRAW_FOUR) devem ter cor BLACK.
 *
 * Exemplo:
 * Card redFive = new Card(CardColor.RED, CardValue.FIVE);
 * Card blueSkip = new Card(CardColor.BLUE, CardValue.SKIP);
 * Card wild = new Card(CardColor.BLACK, CardValue.WILD);
 */
public final class Card {

    // ── Atributos ─────────────────────────────────────────────────────────────

    /** Cor desta carta. */
    private final CardColor color;

    /** Valor desta carta. */
    private final CardValue value;

    // ── Construtor ────────────────────────────────────────────────────────────

    /**
     * Cria uma carta com a cor e o valor dados.
     * Lança IllegalArgumentException se a combinação for inválida.
     */
    public Card(CardColor color, CardValue value) {
        if (color == null || value == null) {
            throw new IllegalArgumentException("Cor e valor não podem ser nulos.");
        }

        boolean isWild = (value == CardValue.WILD || value == CardValue.WILD_DRAW_FOUR);
        boolean isBlack = (color == CardColor.BLACK);

        if (isWild && !isBlack) {
            throw new IllegalArgumentException(
                    "Coringa (" + value + ") precisa ter cor BLACK, recebeu: " + color);
        }
        if (!isWild && isBlack) {
            throw new IllegalArgumentException(
                    "Carta não-coringa (" + value + ") não pode ter cor BLACK.");
        }

        this.color = color;
        this.value = value;
    }

    /** Retorna a cor desta carta. */
    public CardColor getColor() {
        return color;
    }

    /** Retorna o valor desta carta. */
    public CardValue getValue() {
        return value;
    }

    /** Retorna true se esta carta for um coringa (WILD ou WILD_DRAW_FOUR). */
    public boolean isWild() {
        return color == CardColor.BLACK;
    }

    /**
     * Verifica se esta carta pode ser jogada sobre topCard, dada a activeColor.
     *
     * Uma carta é jogável quando:
     * 1. É coringa — pode sempre ser jogada.
     * 2. Tem a mesma cor que activeColor.
     * 3. Tem o mesmo valor que topCard.
     */
    public boolean canPlayOn(Card topCard, CardColor activeColor) {
        if (isWild())
            return true; // regra 1
        if (this.color == activeColor)
            return true; // regra 2
        if (this.value == topCard.getValue())
            return true; // regra 3
        return false;
    }

    /** Formato: [COR VALOR], ex: [RED FIVE], [BLACK WILD]. */
    @Override
    public String toString() {
        return "[" + color + " " + value + "]";
    }

    /** Duas cartas são iguais se tiverem a mesma cor e o mesmo valor. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Card))
            return false;
        Card other = (Card) obj;
        return this.color == other.color && this.value == other.value;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + color.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
