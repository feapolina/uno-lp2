package tests;

import model.Card;
import model.CardColor;
import model.CardValue;
import model.Deck;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public final class DeckSelfTest {

    public static void main(String[] args) {
        testStandardDeckCardCount();
        testCardRules();
        System.out.println("DeckSelfTest: OK");
    }

    private static void testStandardDeckCardCount() {
        Deck deck = Deck.createStandardDeck(new Random(42));
        assertEquals(108, deck.cardsRemaining(), "Baralho padrão UNO deve ter 108 cartas");

        Map<CardColor, Integer> colorCount = new EnumMap<>(CardColor.class);
        for (CardColor color : CardColor.values()) {
            colorCount.put(color, 0);
        }

        while (!deck.isEmpty()) {
            Card card = deck.draw();
            colorCount.put(card.getColor(), colorCount.get(card.getColor()) + 1);
        }

        assertEquals(25, colorCount.get(CardColor.RED), "Quantidade de cartas vermelhas incorreta");
        assertEquals(25, colorCount.get(CardColor.BLUE), "Quantidade de cartas azuis incorreta");
        assertEquals(25, colorCount.get(CardColor.GREEN), "Quantidade de cartas verdes incorreta");
        assertEquals(25, colorCount.get(CardColor.YELLOW), "Quantidade de cartas amarelas incorreta");
        assertEquals(8, colorCount.get(CardColor.BLACK), "Quantidade de coringas incorreta");
    }

    private static void testCardRules() {
        Card top = new Card(CardColor.RED, CardValue.FIVE);
        Card sameColor = new Card(CardColor.RED, CardValue.ONE);
        Card sameValue = new Card(CardColor.BLUE, CardValue.FIVE);
        Card wild = new Card(CardColor.BLACK, CardValue.WILD);
        Card invalid = new Card(CardColor.GREEN, CardValue.NINE);

        assertTrue(sameColor.canPlayOn(top, CardColor.RED), "Carta da mesma cor deve ser jogável");
        assertTrue(sameValue.canPlayOn(top, CardColor.RED), "Carta do mesmo valor deve ser jogável");
        assertTrue(wild.canPlayOn(top, CardColor.RED), "Coringa deve ser jogável");
        assertTrue(!invalid.canPlayOn(top, CardColor.RED), "Carta sem cor/valor compatível não deve ser jogável");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " | esperado=" + expected + " atual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
