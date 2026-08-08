package client;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;

/** Interface Swing propositalmente simples: estado, mão e campo de comando. */
public final class GameWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JLabel roomLabel = new JLabel("Sala: aguardando...");
    private final JLabel currentLabel = new JLabel("Turno: aguardando...");
    private final JLabel topLabel = new JLabel("Topo: N/A");
    private final JLabel activeLabel = new JLabel("Cor ativa: N/A");
    private final JTextArea handArea = new JTextArea(6, 35);
    private final JTextArea eventsArea = new JTextArea(12, 35);
    private final JTextField commandField = new JTextField();
    private final JButton sendButton = new JButton("Enviar");

    public GameWindow(String playerName, Consumer<String> commandSender) {
        super("UNO-LP2 - " + playerName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel status = new JPanel(new GridLayout(4, 1));
        status.setBorder(BorderFactory.createTitledBorder("Estado da partida"));
        status.add(roomLabel);
        status.add(currentLabel);
        status.add(topLabel);
        status.add(activeLabel);

        handArea.setEditable(false);
        handArea.setLineWrap(true);
        handArea.setWrapStyleWord(true);
        JScrollPane handScroll = new JScrollPane(handArea);
        handScroll.setBorder(BorderFactory.createTitledBorder("Sua mão"));

        eventsArea.setEditable(false);
        JScrollPane eventScroll = new JScrollPane(eventsArea);
        eventScroll.setBorder(BorderFactory.createTitledBorder("Eventos"));

        JPanel commandPanel = new JPanel(new BorderLayout(5, 5));
        commandPanel.setBorder(BorderFactory.createTitledBorder(
                "Comando: JOGAR <carta> [cor] [UNO] | COMPRAR | DORMIU | SAIR"));
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);

        Runnable sendAction = () -> {
            String command = commandField.getText().trim();
            if (!command.isEmpty()) {
                commandSender.accept(command);
                commandField.setText("");
            }
        };
        sendButton.addActionListener(e -> sendAction.run());
        commandField.addActionListener(e -> sendAction.run());

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.add(handScroll, BorderLayout.NORTH);
        center.add(eventScroll, BorderLayout.CENTER);

        add(status, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    public void setRoom(String room) {
        roomLabel.setText("Sala: " + room);
    }

    public void setCurrentPlayer(String player) {
        currentLabel.setText("Turno: " + player);
    }

    public void setTopCard(String card) {
        topLabel.setText("Topo: " + card);
    }

    public void setActiveColor(String color) {
        activeLabel.setText("Cor ativa: " + color);
    }

    public void setHand(String hand) {
        handArea.setText(hand == null || hand.isBlank()
                ? "(sem cartas)"
                : hand.replace(",", "   "));
    }

    public void addEvent(String event) {
        eventsArea.append(event + System.lineSeparator());
        eventsArea.setCaretPosition(eventsArea.getDocument().getLength());
    }

    public void disableCommands() {
        commandField.setEnabled(false);
        sendButton.setEnabled(false);
    }
}
