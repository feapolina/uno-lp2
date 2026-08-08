package server;

import java.util.HashMap;
import java.util.Map;

/**
 * Mantém no máximo quatro salas ativas.
 *
 * A implementação usa synchronized + HashMap para permanecer próxima dos
 * exemplos básicos de monitor/sincronização vistos em aula.
 */
public final class RoomManager {

    public static final int MAX_ROOMS = 4;

    private final Map<String, Room> rooms = new HashMap<>();
    private final int turnTimeoutSeconds;
    private final int matchDurationMinutes;
    private int nextRoomNumber = 1;

    public RoomManager(int turnTimeoutSeconds, int matchDurationMinutes) {
        this.turnTimeoutSeconds = turnTimeoutSeconds;
        this.matchDurationMinutes = matchDurationMinutes;
    }

    public synchronized Room createRoom(int expectedPlayers) {
        if (expectedPlayers < 2 || expectedPlayers > 8) {
            throw new IllegalArgumentException("A sala deve ter entre 2 e 8 jogadores.");
        }
        if (rooms.size() >= MAX_ROOMS) {
            throw new IllegalStateException("Limite de 4 salas ativas atingido.");
        }

        String code;
        do {
            code = "SALA" + nextRoomNumber++;
        } while (rooms.containsKey(code));

        Room room = new Room(code, expectedPlayers, turnTimeoutSeconds,
                matchDurationMinutes, this);
        rooms.put(code, room);
        new Thread(room, "Room-" + code).start();
        return room;
    }

    public synchronized Room getRoom(String code) {
        if (code == null) {
            return null;
        }
        return rooms.get(code.trim().toUpperCase());
    }

    synchronized void removeRoom(String code, Room room) {
        Room current = rooms.get(code);
        if (current == room) {
            rooms.remove(code);
        }
    }

    public synchronized int activeRoomCount() {
        return rooms.size();
    }
}
