package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa o estado de uma partida de UNO.
 *
 * Este estado mantém o baralho de compra, o monte de descarte, os jogadores,
 * o jogador atual, a cor ativa e o fluxo de turno. A classe também garante
 * exclusão mútua para operações que alteram o estado do jogo.
 */
public final class GameState {

    private static final int INITIAL_HAND_SIZE = 7;

    private final Deck drawPile;
    private final Deque<Card> discardPile;
    private final List<PlayerState> players;
    private final ReentrantLock lock;
    private final Condition turnUpdated;
    private final Semaphore turnFinished;

    private int currentPlayerIndex;
    private int direction;
    private Card topCard;
    private CardColor activeColor;
    private boolean started;
    private PlayerState winner;

    private GameState(List<String> playerNames, Random random) {
        Objects.requireNonNull(playerNames, "playerNames não pode ser nulo.");
        if (playerNames.size() < 2) {
            throw new IllegalArgumentException("É necessário ao menos dois jogadores.");
        }

        this.drawPile = Deck.createStandardDeck(random);
        this.discardPile = new ArrayDeque<>();
        this.players = new ArrayList<>(playerNames.size());
        this.lock = new ReentrantLock(true);
        this.turnUpdated = lock.newCondition();
        this.turnFinished = new Semaphore(0, true);
        this.currentPlayerIndex = 0;
        this.direction = 1;
        this.started = false;
        this.winner = null;

        dealInitialHands(playerNames);
        prepareInitialTopCard();
        startFirstTurn();
    }

    public static GameState create(List<String> playerNames) {
        return new GameState(playerNames, new Random());
    }

    public static GameState create(List<String> playerNames, Random random) {
        Objects.requireNonNull(random, "random não pode ser nulo.");
        return new GameState(playerNames, random);
    }

    private void dealInitialHands(List<String> playerNames) {
        List<List<Card>> hands = drawPile.dealHands(playerNames.size(), INITIAL_HAND_SIZE);
        for (int i = 0; i < playerNames.size(); i++) {
            PlayerState player = new PlayerState(i, playerNames.get(i));
            player.addCards(hands.get(i));
            players.add(player);
        }
    }

    private void prepareInitialTopCard() {
        Card card = drawPile.draw();
        while (card.isWild()) {
            discardPile.addLast(card);
            card = drawPile.draw();
        }
        discardPile.addFirst(card);
        topCard = card;
        activeColor = card.getColor();
    }

    private void startFirstTurn() {
        currentPlayerIndex = 0;
        started = true;
        currentPlayer().grantTurn();
    }

    public PlayerState currentPlayer() {
        lock.lock();
        try {
            return players.get(currentPlayerIndex);
        } finally {
            lock.unlock();
        }
    }

    public Card getTopCard() {
        lock.lock();
        try {
            return topCard;
        } finally {
            lock.unlock();
        }
    }

    public CardColor getActiveColor() {
        lock.lock();
        try {
            return activeColor;
        } finally {
            lock.unlock();
        }
    }

    public List<PlayerState> getPlayers() {
        lock.lock();
        try {
            return new ArrayList<>(players);
        } finally {
            lock.unlock();
        }
    }

    public boolean isGameOver() {
        lock.lock();
        try {
            return winner != null;
        } finally {
            lock.unlock();
        }
    }

    public PlayerState getWinner() {
        lock.lock();
        try {
            return winner;
        } finally {
            lock.unlock();
        }
    }

    public int getCurrentPlayerIndex() {
        lock.lock();
        try {
            return currentPlayerIndex;
        } finally {
            lock.unlock();
        }
    }

    public void playCard(int playerId, Card card, CardColor declaredColor) {
        Objects.requireNonNull(card, "card não pode ser nulo.");

        lock.lock();
        try {
            ensureStarted();
            PlayerState current = currentPlayer();
            if (current.getPlayerId() != playerId) {
                throw new IllegalStateException("Não é a vez do jogador " + playerId + ".");
            }

            if (!card.canPlayOn(topCard, activeColor)) {
                throw new IllegalStateException("Carta " + card + " não pode ser jogada sobre " + topCard + " com cor ativa " + activeColor + ".");
            }

            Card played = current.playCard(card);
            applyCardEffect(played, declaredColor);
            checkWinner(current);

            if (!isGameOver()) {
                advanceTurn();
            }
        } finally {
            lock.unlock();
        }
    }

    public List<Card> drawCards(int playerId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("A quantidade de cartas para comprar deve ser positiva.");
        }

        lock.lock();
        try {
            ensureStarted();
            PlayerState current = currentPlayer();
            if (current.getPlayerId() != playerId) {
                throw new IllegalStateException("Não é a vez do jogador " + playerId + ".");
            }

            List<Card> drawn = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                replenishDrawPileIfNeeded();
                drawn.add(drawPile.draw());
            }
            current.addCards(drawn);
            advanceTurn();
            return drawn;
        } finally {
            lock.unlock();
        }
    }

    private void applyCardEffect(Card card, CardColor declaredColor) {
        discardPile.addFirst(card);
        topCard = card;

        if (card.isWild()) {
            if (declaredColor == null || declaredColor == CardColor.BLACK) {
                throw new IllegalArgumentException("Cor declarada inválida para carta coringa.");
            }
            activeColor = declaredColor;
        } else {
            activeColor = card.getColor();
        }

        switch (card.getValue()) {
            case SKIP:
                advanceTurnBy(1);
                break;
            case REVERSE:
                if (players.size() == 2) {
                    advanceTurnBy(1);
                } else {
                    direction *= -1;
                }
                break;
            case DRAW_TWO:
                drawForNextPlayer(2);
                break;
            case WILD_DRAW_FOUR:
                drawForNextPlayer(4);
                break;
            default:
                break;
        }
    }

    private void drawForNextPlayer(int cardsToDraw) {
        int nextIndex = indexAfter(currentPlayerIndex, 1);
        PlayerState nextPlayer = players.get(nextIndex);
        List<Card> cards = new ArrayList<>(cardsToDraw);
        for (int i = 0; i < cardsToDraw; i++) {
            replenishDrawPileIfNeeded();
            cards.add(drawPile.draw());
        }
        nextPlayer.addCards(cards);
        currentPlayerIndex = nextIndex;
        advanceTurnBy(1);
    }

    private void advanceTurn() {
        currentPlayerIndex = indexAfter(currentPlayerIndex, 1);
        currentPlayer().grantTurn();
        turnUpdated.signalAll();
    }

    private void advanceTurnBy(int skipCount) {
        for (int i = 0; i <= skipCount; i++) {
            currentPlayerIndex = indexAfter(currentPlayerIndex, 1);
        }
        currentPlayer().grantTurn();
        turnUpdated.signalAll();
    }

    private int indexAfter(int currentIndex, int steps) {
        int size = players.size();
        int next = (currentIndex + direction * steps) % size;
        return next < 0 ? next + size : next;
    }

    private void replenishDrawPileIfNeeded() {
        if (!drawPile.isEmpty()) {
            return;
        }

        if (discardPile.size() <= 1) {
            throw new IllegalStateException("Não há cartas suficientes para reposição do baralho de compra.");
        }

        Card top = discardPile.removeFirst();
        List<Card> shuffled = new ArrayList<>(discardPile);
        discardPile.clear();
        Collections.shuffle(shuffled, new Random());
        for (Card card : shuffled) {
            drawPile.cards.addLast(card);
        }
        discardPile.addFirst(top);
    }

    private void checkWinner(PlayerState player) {
        if (player.cardsInHand() == 0) {
            winner = player;
        }
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("A partida ainda não foi iniciada.");
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return "GameState[players=" + players.size() + ", current=" + currentPlayer().getPlayerName()
                    + ", topCard=" + topCard + ", activeColor=" + activeColor + ", drawPile=" + drawPile.cardsRemaining() + "]";
        } finally {
            lock.unlock();
        }
    }
}
