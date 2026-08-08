package tests;

import model.Card;
import model.CardColor;
import model.CardValue;
import model.GameState;
import model.PlayerState;

import java.util.List;
import java.util.Random;

public final class GameStateSelfTest {

    public static void main(String[] args) {
        testInitialState();
        testTurnAdvanceByDraw();
        testPlayOrDrawFlow();
        System.out.println("GameStateSelfTest: OK");
    }

    private static void testInitialState() {
        GameState state = GameState.create(List.of("Ana", "Beto"), new Random(1));
        List<PlayerState> players = state.getPlayers();

        assertEquals(2, players.size(), "Partida com 2 jogadores deve ter 2 PlayerState");
        assertEquals(7, players.get(0).cardsInHand(), "Mão inicial do jogador 0 deve ter 7 cartas");
        assertEquals(7, players.get(1).cardsInHand(), "Mão inicial do jogador 1 deve ter 7 cartas");
        assertTrue(state.getTopCard().getColor() != CardColor.BLACK, "Carta inicial não deve ser coringa");
        assertEquals(0, state.getCurrentPlayerIndex(), "Primeiro turno deve ser do jogador 0");
    }

    private static void testTurnAdvanceByDraw() {
        GameState state = GameState.create(List.of("Ana", "Beto"), new Random(2));
        int current = state.getCurrentPlayerIndex();
        state.drawCards(current, 1);
        int next = state.getCurrentPlayerIndex();
        assertTrue(next != current, "Após comprar, o turno deve avançar para o outro jogador");
    }

    private static void testPlayOrDrawFlow() {
        GameState state = GameState.create(List.of("Ana", "Beto"), new Random(3));
        int currentIndex = state.getCurrentPlayerIndex();
        PlayerState current = state.getPlayers().get(currentIndex);

        Card top = state.getTopCard();
        CardColor active = state.getActiveColor();

        Card playable = null;
        for (Card card : current.getHandSnapshot()) {
            if (card.canPlayOn(top, active)) {
                playable = card;
                break;
            }
        }

        int before = current.cardsInHand();
        if (playable != null) {
            CardColor declaredColor = null;
            if (playable.getValue() == CardValue.WILD || playable.getValue() == CardValue.WILD_DRAW_FOUR) {
                declaredColor = CardColor.RED;
            }
            int remaining = state.playCard(current.getPlayerId(), playable, declaredColor);
            assertEquals(before - 1, remaining, "Jogar carta normal deve reduzir a mão em 1");
        } else {
            state.drawCards(current.getPlayerId(), 1);
            int after = current.cardsInHand();
            assertEquals(before + 1, after, "Sem carta jogável, comprar 1 deve aumentar a mão em 1");
        }
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
