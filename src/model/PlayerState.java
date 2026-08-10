package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Estado simples e thread-safe de um jogador.
 * Guarda a mão do jogador, o identificador e a permissão para agir no turno atual.
 */
public final class PlayerState {

    private final int playerId;
    private final String playerName;
    private final List<Card> hand = new ArrayList<>();
    private final ReentrantLock handLock = new ReentrantLock(true);
    private final Semaphore turnPermission = new Semaphore(0, true);

    public PlayerState(int playerId, String playerName) {
        if (playerId < 0) {
            throw new IllegalArgumentException("O id do jogador não pode ser negativo.");
        }
        this.playerId = playerId;
        this.playerName = Objects.requireNonNull(playerName,
                "playerName não pode ser nulo.");
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    /** Libera exatamente uma execução de turno para a thread desse jogador. */
    public void grantTurn() {
        turnPermission.release();
    }

    /** Bloqueia sem espera ocupada até o servidor conceder o turno. */
    public void waitForTurn() {
        turnPermission.acquireUninterruptibly();
    }

    public void addCards(List<Card> cardsToAdd) {
        Objects.requireNonNull(cardsToAdd, "cardsToAdd não pode ser nulo.");
        for (Card card : cardsToAdd) {
            if (card == null) {
                throw new IllegalArgumentException(
                        "A lista de cartas não pode conter valores nulos.");
            }
        }

        handLock.lock();
        try {
            hand.addAll(cardsToAdd);
        } finally {
            handLock.unlock();
        }
    }

    public Card playCard(Card card) {
        Objects.requireNonNull(card, "card não pode ser nulo.");
        handLock.lock();
        try {
            if (!hand.remove(card)) {
                throw new IllegalStateException(
                        "Carta não encontrada na mão do jogador: " + card);
            }
            return card;
        } finally {
            handLock.unlock();
        }
    }

    public List<Card> getHandSnapshot() {
        handLock.lock();
        try {
            return new ArrayList<>(hand);
        } finally {
            handLock.unlock();
        }
    }

    public int cardsInHand() {
        handLock.lock();
        try {
            return hand.size();
        } finally {
            handLock.unlock();
        }
    }

    public List<Card> removeAllCards() {
        handLock.lock();
        try {
            List<Card> removed = new ArrayList<>(hand);
            hand.clear();
            return removed;
        } finally {
            handLock.unlock();
        }
    }

    @Override
    public String toString() {
        return "PlayerState[id=" + playerId + ", name=" + playerName
                + ", cards=" + cardsInHand() + "]";
    }
}
