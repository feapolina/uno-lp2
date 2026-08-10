package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa o estado de uma partida de UNO.
 *
 * Este estado mantém o baralho de compra, o monte de descarte, os jogadores,
 * o jogador atual, a cor ativa e o fluxo de turno. A classe garante exclusão
 * mútua para todas as operações que alteram o estado da partida.
 *
 * O servidor usa esta classe como única fonte de verdade da partida.
 */
public final class GameState {

    private static final int INITIAL_HAND_SIZE = 7;

    private final Deck drawPile;
    private final Deque<Card> discardPile;
    private final List<PlayerState> players;
    private final ReentrantLock lock;
    private final Random random;

    private int currentPlayerIndex;
    private int direction;
    private Card topCard;
    private CardColor activeColor;
    private boolean started;
    private PlayerState winner;
    private boolean aborted;
    private String endReason;

    private GameState(List<String> playerNames, Random random) {
        Objects.requireNonNull(playerNames, "playerNames não pode ser nulo.");
        if (playerNames.size() < 2) {
            throw new IllegalArgumentException("É necessário ao menos dois jogadores.");
        }

        this.random      = Objects.requireNonNull(random, "random não pode ser nulo.");
        this.drawPile    = Deck.createStandardDeck(random);
        this.discardPile = new ArrayDeque<>();
        this.players     = new ArrayList<>(playerNames.size());
        this.lock        = new ReentrantLock(true);
        this.currentPlayerIndex = 0;
        this.direction   = 1;
        this.started     = false;
        this.winner      = null;
        this.aborted     = false;
        this.endReason   = null;

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

    // ── Inicialização ─────────────────────────────────────────────────────────

    private void dealInitialHands(List<String> playerNames) {
        List<List<Card>> hands = drawPile.dealHands(playerNames.size(), INITIAL_HAND_SIZE);
        for (int i = 0; i < playerNames.size(); i++) {
            PlayerState player = new PlayerState(i, playerNames.get(i));
            player.addCards(hands.get(i));
            players.add(player);
        }
    }

    /** Coringas não são usados como carta inicial do descarte. */
    private void prepareInitialTopCard() {
        List<Card> rejectedWilds = new ArrayList<>();
        Card card = drawPile.draw();
        while (card.isWild()) {
            rejectedWilds.add(card);
            card = drawPile.draw();
        }
        if (!rejectedWilds.isEmpty()) {
            drawPile.addCardsToBottom(rejectedWilds);
        }
        discardPile.addFirst(card);
        topCard = card;
        activeColor = card.getColor();
    }

    private void startFirstTurn() {
        currentPlayerIndex = 0;
        started = true;
        players.get(0).grantTurn();
    }

    // ── Leitores públicos (thread-safe) ───────────────────────────────────────

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
            return winner != null || aborted;
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

    public String getEndReason() {
        lock.lock();
        try {
            return endReason;
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

    // ── Ações de jogo ─────────────────────────────────────────────────────────

    /**
     * Joga uma carta da mão do jogador identificado por playerId.
     *
     * O retorno informa quantas cartas restaram na mão após a jogada,
     * permitindo aplicar a regra de UNO sem ler um estado intermediário.
     *
     * @return número de cartas restantes na mão do jogador que jogou
     */
    public int playCard(int playerId, Card card, CardColor declaredColor) {
        Objects.requireNonNull(card, "card não pode ser nulo.");

        lock.lock();
        try {
            ensureStarted();
            PlayerState current = players.get(currentPlayerIndex);
            if (current.getPlayerId() != playerId) {
                throw new IllegalStateException("Não é a vez do jogador " + playerId + ".");
            }

            if (card.isWild() && (declaredColor == null || declaredColor == CardColor.BLACK)) {
                throw new IllegalArgumentException("Carta coringa exige uma cor declarada válida.");
            }

            if (card.getValue() == CardValue.WILD_DRAW_FOUR && hasColorAlternativeForWildDrawFour(current)) {
                throw new IllegalStateException(
                        "Coringa compra quatro só pode ser jogado quando não há carta da cor ativa na mão.");
            }

            if (!card.canPlayOn(topCard, activeColor)) {
                throw new IllegalStateException(
                        "Carta " + card + " não pode ser jogada sobre " + topCard
                        + " com cor ativa " + activeColor + ".");
            }

            Card played = current.playCard(card);
            int remainingCards = current.cardsInHand();

            boolean turnHandled = applyCardEffect(played, declaredColor);
            checkWinner(current);

            if (winner == null && !aborted && !turnHandled) {
                advanceTurn();
            }
            return remainingCards;
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
            PlayerState current = players.get(currentPlayerIndex);
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

    /**
     * Adiciona uma penalidade de cartas a um jogador sem alterar o turno.
     * Usado pela regra de "DORMIU" do UNO.
     */
    public List<Card> addPenaltyCards(int playerId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("A penalidade deve ser positiva.");
        }

        lock.lock();
        try {
            ensureStarted();
            if (playerId < 0 || playerId >= players.size()) {
                throw new IllegalArgumentException("Jogador inválido: " + playerId);
            }

            List<Card> drawn = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                replenishDrawPileIfNeeded();
                drawn.add(drawPile.draw());
            }
            players.get(playerId).addCards(drawn);
            return drawn;
        } finally {
            lock.unlock();
        }
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    private boolean hasColorAlternativeForWildDrawFour(PlayerState player) {
        for (Card handCard : player.getHandSnapshot()) {
            if (!handCard.isWild() && handCard.getColor() == activeColor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aplica o efeito da carta jogada sobre o estado do jogo.
     *
     * Retorna true quando o efeito da carta já definiu o próximo turno.
     *
     * Regras por tipo de carta:
     *  - Normal / WILD: false → advanceTurn() avança 1 posição normalmente.
     *  - SKIP          : true → avança 2 (pula próximo, dá vez ao seguinte).
     *  - REVERSE (2p)  : true → mesmo jogador joga de novo (avança 0 efetivo).
     *  - REVERSE (3p+) : false → inverte direção, advanceTurn() vai para o anterior.
     *  - DRAW_TWO      : true → próximo compra 2 e perde a vez (avança 2).
     *  - WILD_DRAW_FOUR: true → próximo compra 4 e perde a vez (avança 2).
     */
    private boolean applyCardEffect(Card card, CardColor declaredColor) {
        // Coloca a carta jogada no topo do descarte para formar a nova base da rodada.
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
                // Pula o próximo e dá a vez ao jogador seguinte (+2 do atual).
                currentPlayerIndex = indexAfter(currentPlayerIndex, 2);
                players.get(currentPlayerIndex).grantTurn();
                return true;

            case REVERSE:
                if (players.size() == 2) {
                    // Em 2 jogadores REVERSE age como SKIP: o mesmo jogador joga de novo.
                    players.get(currentPlayerIndex).grantTurn();
                    return true;
                }
                // 3+ jogadores: inverte a direção e deixa advanceTurn() ir para o anterior.
                direction *= -1;
                return false;

            case DRAW_TWO:
                applyDrawPenalty(2);
                return true;

            case WILD_DRAW_FOUR:
                applyDrawPenalty(4);
                return true;

            default:
                return false;
        }
    }

    /**
     * Distribui cartas ao próximo jogador como penalidade e avança o turno para
     * o jogador seguinte ao penalizado (ou seja, avança 2 posições no total a
     * partir do jogador atual).
     */
    private void applyDrawPenalty(int cardsToDraw) {
        // Cartas como DRAW_TWO e WILD_DRAW_FOUR penalizam o próximo jogador.
        int penalizedIndex = indexAfter(currentPlayerIndex, 1);
        PlayerState penalized = players.get(penalizedIndex);

        List<Card> cards = new ArrayList<>(cardsToDraw);
        for (int i = 0; i < cardsToDraw; i++) {
            replenishDrawPileIfNeeded();
            cards.add(drawPile.draw());
        }
        penalized.addCards(cards);

        // Pula o jogador penalizado: turno vai para o próximo depois dele.
        currentPlayerIndex = indexAfter(penalizedIndex, 1);
        players.get(currentPlayerIndex).grantTurn();
    }

    /** Avança o turno normalmente (+1 na direção atual) e notifica. */
    private void advanceTurn() {
        // Move o turno para o próximo jogador conforme a direção atual.
        currentPlayerIndex = indexAfter(currentPlayerIndex, 1);
        players.get(currentPlayerIndex).grantTurn();
    }

    private int indexAfter(int currentIndex, int steps) {
        int size = players.size();
        int next = (currentIndex + direction * steps) % size;
        return next < 0 ? next + size : next;
    }

    /** Reaproveita o descarte, preservando a carta do topo. */
    private void replenishDrawPileIfNeeded() {
        if (!drawPile.isEmpty()) {
            return;
        }

        if (discardPile.size() <= 1) {
            throw new IllegalStateException(
                    "Não há cartas suficientes para reposição do baralho de compra.");
        }

        Card top = discardPile.removeFirst();
        List<Card> shuffled = new ArrayList<>(discardPile);
        discardPile.clear();
        Collections.shuffle(shuffled, random);
        drawPile.addCardsToBottom(shuffled);
        discardPile.addFirst(top);
    }

    private void checkWinner(PlayerState player) {
        if (player.cardsInHand() == 0) {
            winner = player;
            // Libera handlers que estejam bloqueados aguardando o próximo turno.
            for (PlayerState other : players) {
                other.grantTurn();
            }
        }
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("A partida ainda não foi iniciada.");
        }
    }

    public void abortGame(String reason) {
        lock.lock();
        try {
            if (winner != null || aborted) {
                return;
            }

            aborted = true;
            endReason = (reason == null || reason.isBlank())
                    ? "Partida encerrada pelo servidor."
                    : reason;

            // Libera possíveis threads bloqueadas aguardando vez.
            for (PlayerState player : players) {
                player.grantTurn();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return "GameState[players=" + players.size()
                    + ", current=" + players.get(currentPlayerIndex).getPlayerName()
                    + ", topCard=" + topCard
                    + ", activeColor=" + activeColor
                    + ", drawPile=" + drawPile.cardsRemaining() + "]";
        } finally {
            lock.unlock();
        }
    }
}
