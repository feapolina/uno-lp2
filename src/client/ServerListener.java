package client;

import shared.Protocol;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Thread responsável por escutar mensagens vindas do servidor.
 * Ela atualiza a interface do cliente sempre que chega uma nova informação.
 */
public final class ServerListener implements Runnable {

    private final BufferedReader in;
    private final GameWindow window;

    public ServerListener(BufferedReader in, GameWindow window) {
        this.in = in;
        this.window = window;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                process(line);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> window.addEvent(
                    "Conexão encerrada: " + e.getMessage()));
        }
    }

    private void process(String line) {
        String[] parts = Protocol.parseMessage(line);
        if (parts.length == 0) {
            return;
        }
        String command = parts[0];
        String payload = parts.length > 1
                ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length))
                : "";

        SwingUtilities.invokeLater(() -> {
            if (Protocol.MSG_SALA_CRIADA.equals(command)
                    || Protocol.SALA_JOIN_SUCCESS.equals(command)) {
                if (parts.length > 1) {
                    window.setRoom(parts[1]);
                }
            } else if (Protocol.MSG_TOPO.equals(command)) {
                window.setTopCard(payload);
            } else if (Protocol.MSG_ATIVA.equals(command)) {
                window.setActiveColor(payload);
            } else if (Protocol.MSG_ATUAL.equals(command)) {
                window.setCurrentPlayer(payload);
            } else if (Protocol.MSG_MAO.equals(command)) {
                window.setHand(payload);
            } else {
                window.addEvent(payload.isBlank() ? command : command + " - " + payload);
            }

            if (Protocol.FIM_JOGO.equals(command)
                    || Protocol.FIM_JOGO_PARTIDA_ENCERRADA.equals(command)) {
                window.disableCommands();
            }
        });
    }
}
