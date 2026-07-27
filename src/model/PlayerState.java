package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Estado de um jogador dentro da partida UNO.
 *
 * Esta classe é responsável por armazenar a mão de cartas de um jogador e por
 * garantir exclusão mútua ao acessar ou modificar esta mão. Também fornece
 * mecanismos de controle de turno, que podem ser usados pelo servidor para
 * coordenar o fluxo de jogo entre múltiplas threads.
 */
public final class PlayerState {

    private final int playerId;
    private final String playerName;

    private final List<Card> hand;
    private final ReentrantLock handLock;
    private final Condition handNotEmpty;

    private final Semaphore turnPermission;
    private final Semaphore actionPermission;

    public PlayerState(int playerId, String playerName) {
        if (playerId < 0) {
            throw new IllegalArgumentException("O id do jogador não pode ser negativo.");
        }
        this.playerId = playerId;
        this.playerName = Objects.requireNonNull(playerName, "playerName não pode ser nulo.");
        this.hand = new ArrayList<>();
        this.handLock = new ReentrantLock(true);
        this.handNotEmpty = handLock.newCondition();
        this.turnPermission = new Semaphore(0, true);
        this.actionPermission = new Semaphore(1, true);
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    /**
     * Concede a vez ao jogador. O jogador só pode agir após receber essa permissão.
     */
    public void grantTurn() {
        turnPermission.release();
    }

    /**
     * Bloqueia até que seja a vez deste jogador.
     */
    public void waitForTurn() {
        turnPermission.acquireUninterruptibly();
    }

    /**
     * Retira a permissão de turno sem bloquear, útil para limpar estados após o turno.
     */
    public void clearTurnPermission() {
        turnPermission.drainPermits();
    }

    /**
     * Adiciona cartas à mão do jogador de forma thread-safe.
     */
    public void addCards(List<Card> cardsToAdd) {
        Objects.requireNonNull(cardsToAdd, "cardsToAdd não pode ser nulo.");
        if (cardsToAdd.contains(null)) {
            throw new IllegalArgumentException("A lista de cartas não pode conter valores nulos.");
        }

        handLock.lock();
        try {
            hand.addAll(cardsToAdd);
            handNotEmpty.signalAll();
        } finally {
            handLock.unlock();
        }
    }

    /**
     * Compra uma carta para a mão do jogador.
     */
    public void drawCard(Card card) {
        Objects.requireNonNull(card, "card não pode ser nulo.");
        handLock.lock();
        try {
            hand.add(card);
            handNotEmpty.signalAll();
        } finally {
            handLock.unlock();
        }
    }

    /**
     * Joga uma carta da mão. Garante exclusão mútua durante a remoção.
     */
    public Card playCard(Card card) {
        Objects.requireNonNull(card, "card não pode ser nulo.");
        actionPermission.acquireUninterruptibly();
        handLock.lock();
        try {
            if (!hand.remove(card)) {
                throw new IllegalStateException("Carta não encontrada na mão do jogador: " + card);
            }
            return card;
        } finally {
            handLock.unlock();
            actionPermission.release();
        }
    }

    /**
     * Retorna uma cópia thread-safe da mão atual do jogador.
     */
    public List<Card> getHandSnapshot() {
        handLock.lock();
        try {
            return new ArrayList<>(hand);
        } finally {
            handLock.unlock();
        }
    }

    /**
     * Retorna o número de cartas atualmente na mão.
     */
    public int cardsInHand() {
        handLock.lock();
        try {
            return hand.size();
        } finally {
            handLock.unlock();
        }
    }

    /**
     * Aguarda até que o jogador tenha ao menos uma carta na mão.
     */
    public void waitForCard() {
        handLock.lock();
        try {
            while (hand.isEmpty()) {
                handNotEmpty.awaitUninterruptibly();
            }
        } finally {
            handLock.unlock();
        }
    }

    /**
     * Verifica se existe alguma carta jogável na mão contra a carta de topo e a cor ativa.
     */
    public boolean hasPlayableCard(Card topCard, CardColor activeColor) {
        Objects.requireNonNull(topCard, "topCard não pode ser nulo.");
        Objects.requireNonNull(activeColor, "activeColor não pode ser nulo.");

        handLock.lock();
        try {
            return hand.stream().anyMatch(card -> card.canPlayOn(topCard, activeColor));
        } finally {
            handLock.unlock();
        }
    }

    /**
     * Remove todas as cartas da mão do jogador e retorna a lista removida.
     */
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
        return "PlayerState[id=" + playerId + ", name=" + playerName + ", cards=" + cardsInHand() + "]";
    }
}
