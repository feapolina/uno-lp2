package tests;

import model.Card;
import model.CardColor;
import model.CardValue;
import model.GameState;
import model.PlayerState;

import java.util.List;
import java.util.Random;

/** Testes de regressão para regras e concorrência do estado do jogo. */
public final class GameStateEdgeSelfTest {

    public static void main(String[] args) throws Exception {
        testWildWithoutDeclaredColorDoesNotMutateState();
        testWildDrawFourRestriction();
        testPenaltyDoesNotAdvanceTurn();
        testActionCardTurnFlow();
        testWinnerReleasesWaitingHandlers();
        System.out.println("GameStateEdgeSelfTest: OK");
    }

    private static void testWildWithoutDeclaredColorDoesNotMutateState() {
        GameState state = findStateWithWild();
        PlayerState current = state.currentPlayer();
        Card wild = current.getHandSnapshot().stream()
                .filter(card -> card.getValue() == CardValue.WILD)
                .findFirst()
                .orElseThrow();

        int beforeCards = current.cardsInHand();
        Card beforeTop = state.getTopCard();

        expectException(IllegalArgumentException.class,
                () -> state.playCard(current.getPlayerId(), wild, null),
                "Coringa sem cor declarada deve ser rejeitado");

        assertEquals(beforeCards, current.cardsInHand(),
                "Jogada inválida não pode remover carta da mão");
        assertEquals(beforeTop, state.getTopCard(),
                "Jogada inválida não pode alterar o topo");
    }

    private static void testWildDrawFourRestriction() {
        GameState state = GameState.create(List.of("Ana", "Beto"), new Random(10));
        PlayerState current = state.currentPlayer();
        CardColor active = state.getActiveColor();

        current.removeAllCards();
        Card sameColor = new Card(active, CardValue.ONE);
        Card drawFour = new Card(CardColor.BLACK, CardValue.WILD_DRAW_FOUR);
        current.addCards(List.of(sameColor, drawFour));

        expectException(IllegalStateException.class,
                () -> state.playCard(current.getPlayerId(), drawFour, CardColor.RED),
                "+4 não pode ser usado quando existe carta da cor ativa");
        assertEquals(2, current.cardsInHand(),
                "+4 ilegal não pode alterar a mão");
    }

    private static void testPenaltyDoesNotAdvanceTurn() {
        GameState state = GameState.create(List.of("Ana", "Beto"), new Random(11));
        int beforeTurn = state.getCurrentPlayerIndex();
        PlayerState punished = state.getPlayers().get(1);
        int beforeCards = punished.cardsInHand();

        state.addPenaltyCards(1, 2);

        assertEquals(beforeTurn, state.getCurrentPlayerIndex(),
                "Penalidade de DORMIU não deve trocar o turno");
        assertEquals(beforeCards + 2, punished.cardsInHand(),
                "Penalidade deve adicionar duas cartas");
    }

    private static void testActionCardTurnFlow() {
        GameState skipState = GameState.create(
                List.of("A", "B", "C"), new Random(12));
        PlayerState skipPlayer = skipState.currentPlayer();
        Card skip = new Card(skipState.getActiveColor(), CardValue.SKIP);
        skipPlayer.removeAllCards();
        skipPlayer.addCards(List.of(skip, new Card(skipState.getActiveColor(), CardValue.ONE)));
        skipState.playCard(skipPlayer.getPlayerId(), skip, null);
        assertEquals(2, skipState.getCurrentPlayerIndex(),
                "SKIP com 3 jogadores deve pular o jogador 1");

        GameState reverseState = GameState.create(
                List.of("A", "B", "C"), new Random(13));
        PlayerState reversePlayer = reverseState.currentPlayer();
        Card reverse = new Card(reverseState.getActiveColor(), CardValue.REVERSE);
        reversePlayer.removeAllCards();
        reversePlayer.addCards(List.of(reverse,
                new Card(reverseState.getActiveColor(), CardValue.ONE)));
        reverseState.playCard(reversePlayer.getPlayerId(), reverse, null);
        assertEquals(2, reverseState.getCurrentPlayerIndex(),
                "REVERSE com 3 jogadores deve inverter para o jogador 2");

        GameState drawState = GameState.create(
                List.of("A", "B", "C"), new Random(14));
        PlayerState drawPlayer = drawState.currentPlayer();
        PlayerState penalized = drawState.getPlayers().get(1);
        int before = penalized.cardsInHand();
        Card drawTwo = new Card(drawState.getActiveColor(), CardValue.DRAW_TWO);
        drawPlayer.removeAllCards();
        drawPlayer.addCards(List.of(drawTwo,
                new Card(drawState.getActiveColor(), CardValue.ONE)));
        drawState.playCard(drawPlayer.getPlayerId(), drawTwo, null);
        assertEquals(before + 2, penalized.cardsInHand(),
                "DRAW_TWO deve adicionar duas cartas ao próximo jogador");
        assertEquals(2, drawState.getCurrentPlayerIndex(),
                "Jogador penalizado com DRAW_TWO deve perder a vez");
    }

    private static void testWinnerReleasesWaitingHandlers() throws Exception {
        GameState state = GameState.create(List.of("Ana", "Beto"), new Random(15));
        PlayerState current = state.currentPlayer();
        PlayerState waiting = state.getPlayers().get(1);

        Card winningCard = new Card(state.getActiveColor(), CardValue.ZERO);
        current.removeAllCards();
        current.addCards(List.of(winningCard));

        Thread waiter = new Thread(waiting::waitForTurn, "winner-release-test");
        waiter.start();
        Thread.sleep(50);
        assertTrue(waiter.isAlive(), "Outro jogador deve estar aguardando o turno");

        state.playCard(current.getPlayerId(), winningCard, null);
        waiter.join(1000);

        assertTrue(state.isGameOver(), "Partida deve terminar quando a mão zera");
        assertTrue(!waiter.isAlive(),
                "Fim de jogo deve liberar threads bloqueadas aguardando turno");
    }

    private static GameState findStateWithWild() {
        for (int seed = 0; seed < 10_000; seed++) {
            GameState state = GameState.create(List.of("Ana", "Beto"), new Random(seed));
            boolean hasWild = state.currentPlayer().getHandSnapshot().stream()
                    .anyMatch(card -> card.getValue() == CardValue.WILD);
            if (hasWild) {
                return state;
            }
        }
        throw new AssertionError("Não foi possível localizar estado determinístico com WILD");
    }

    private static void expectException(Class<? extends Throwable> expected,
                                        Runnable action,
                                        String message) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError(message + " | exceção inesperada=" + actual, actual);
        }
        throw new AssertionError(message + " | nenhuma exceção lançada");
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
