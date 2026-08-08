package client;

import shared.Protocol;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** Cliente Swing mínimo, sem dependências externas. */
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java client.ClientMain <host> <porta> <nome>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];

        SwingUtilities.invokeLater(() -> connect(host, port, name));
    }

    private static void connect(String host, int port, String name) {
        try {
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Consome a mensagem inicial do lobby.
            String lobby = in.readLine();
            if (lobby == null) {
                socket.close();
                return;
            }

            Object[] options = {"Criar sala", "Entrar em sala"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Como deseja entrar na partida?",
                    "UNO-LP2",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 0) {
                String count = JOptionPane.showInputDialog(null,
                        "Número de jogadores da sala (2 a 8):", "2");
                if (count == null) {
                    socket.close();
                    return;
                }
                out.println(Protocol.buildMessage(
                        Protocol.CMD_CRIAR_SALA, name, count.trim()));
            } else if (choice == 1) {
                String code = JOptionPane.showInputDialog(null,
                        "Código da sala (ex.: SALA1):");
                if (code == null) {
                    socket.close();
                    return;
                }
                out.println(Protocol.buildMessage(
                        Protocol.CMD_ENTRAR_SALA, code.trim().toUpperCase(), name));
            } else {
                socket.close();
                return;
            }

            GameWindow window = new GameWindow(name, command -> {
                String[] tokens = command.trim().split("\\s+");
                String[] arguments = new String[Math.max(0, tokens.length - 1)];
                if (arguments.length > 0) {
                    System.arraycopy(tokens, 1, arguments, 0, arguments.length);
                }
                out.println(Protocol.buildMessage(tokens[0].toUpperCase(), arguments));
            });
            window.addEvent(String.join(" ", Protocol.parseMessage(lobby)));
            window.setVisible(true);

            Thread listener = new Thread(new ServerListener(in, window), "ServerListener-Swing");
            listener.setDaemon(true);
            listener.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Falha ao conectar: " + e.getMessage(),
                    "UNO-LP2",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
