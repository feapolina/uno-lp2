package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Representa o baralho principal do UNO.
 *
 * O baralho central contém todas as cartas restantes que ainda não foram
 * distribuídas para os jogadores ou descartadas. Ele é embaralhado e usado
 * para entregar as mãos iniciais e para fornecer cartas durante a partida.
 */
public final class Deck {

    private static final List<CardValue> COLOR_VALUES = Arrays.asList(
            CardValue.ONE,
            CardValue.TWO,
            CardValue.THREE,
            CardValue.FOUR,
            CardValue.FIVE,
            CardValue.SIX,
            CardValue.SEVEN,
            CardValue.EIGHT,
            CardValue.NINE,
            CardValue.SKIP,
            CardValue.REVERSE,
            CardValue.DRAW_TWO);

    private final Deque<Card> cards;
    private final Random random;

    /**
     * Cria um baralho vazio que pode ser preenchido e embaralhado.
     */
    public Deck() {
        this(new Random());
    }

    /**
     * Cria um baralho vazio usando o gerador de números aleatórios fornecido.
     */
    public Deck(Random random) {
        this.random = Objects.requireNonNull(random, "Random não pode ser nulo.");
        this.cards = new ArrayDeque<>();
    }

    /**
     * Cria um baralho UNO padrão completo e já embaralhado.
     */
    public static Deck createStandardDeck() {
        return createStandardDeck(new Random());
    }

    /**
     * Cria um baralho UNO padrão completo e já embaralhado, usando o Random fornecido.
     */
    public static Deck createStandardDeck(Random random) {
        Deck deck = new Deck(random);
        for (CardColor color : CardColor.values()) {
            if (color == CardColor.BLACK) {
                continue;
            }

            deck.addCard(new Card(color, CardValue.ZERO));

            for (CardValue value : COLOR_VALUES) {
                deck.addCard(new Card(color, value));
                deck.addCard(new Card(color, value));
            }
        }

        for (int i = 0; i < 4; i++) {
            deck.addCard(new Card(CardColor.BLACK, CardValue.WILD));
            deck.addCard(new Card(CardColor.BLACK, CardValue.WILD_DRAW_FOUR));
        }

        deck.shuffle();
        return deck;
    }

    private void addCard(Card card) {
        cards.addLast(card);
    }

    /**
     * Embaralha as cartas restantes no baralho.
     */
    public void shuffle() {
        List<Card> shuffled = new ArrayList<>(cards);
        Collections.shuffle(shuffled, random);
        cards.clear();
        cards.addAll(shuffled);
    }

    /**
     * Retira a carta do topo do baralho central.
     */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Não há cartas suficientes no baralho para comprar.");
        }
        return cards.removeFirst();
    }

    /**
     * Retira várias cartas do topo do baralho central.
     */
    public List<Card> draw(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("A quantidade de cartas a comprar não pode ser negativa.");
        }
        if (count > cards.size()) {
            throw new IllegalStateException("Não há cartas suficientes no baralho para comprar " + count + " cartas.");
        }

        List<Card> drawn = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            drawn.add(draw());
        }
        return drawn;
    }

    /**
     * Distribui mãos iniciais para o número de jogadores informado.
     *
     * Cada jogador recebe exatamente cardsPerPlayer cartas. A distribuição
     * é feita em "rodadas", como no UNO real: cada jogador recebe uma carta por
     * vez até completar sua mão.
     */
    public List<List<Card>> dealHands(int numberOfPlayers, int cardsPerPlayer) {
        if (numberOfPlayers <= 0) {
            throw new IllegalArgumentException("O número de jogadores deve ser maior que zero.");
        }
        if (cardsPerPlayer < 0) {
            throw new IllegalArgumentException("O número de cartas por jogador não pode ser negativo.");
        }

        int totalCardsNeeded = numberOfPlayers * cardsPerPlayer;
        if (totalCardsNeeded > cards.size()) {
            throw new IllegalStateException("Não há cartas suficientes no baralho para distribuir "
                    + totalCardsNeeded + " cartas para " + numberOfPlayers + " jogadores.");
        }

        List<List<Card>> hands = new ArrayList<>(numberOfPlayers);
        for (int i = 0; i < numberOfPlayers; i++) {
            hands.add(new ArrayList<>(cardsPerPlayer));
        }

        for (int cardIndex = 0; cardIndex < cardsPerPlayer; cardIndex++) {
            for (List<Card> hand : hands) {
                hand.add(draw());
            }
        }

        return hands;
    }

    /**
     * Retorna o número de cartas atualmente disponíveis no baralho central.
     */
    public int cardsRemaining() {
        return cards.size();
    }

    /**
     * Adiciona cartas ao fundo do baralho de compra.
     */
    public void addCardsToBottom(List<Card> cardsToAdd) {
        Objects.requireNonNull(cardsToAdd, "cardsToAdd não pode ser nulo.");
        if (cardsToAdd.contains(null)) {
            throw new IllegalArgumentException("A lista de cartas não pode conter valores nulos.");
        }
        cards.addAll(cardsToAdd);
    }

    /**
     * Retorna true se não houver mais cartas no baralho central.
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public String toString() {
        return "Deck[remaining=" + cards.size() + "]";
    }
}
