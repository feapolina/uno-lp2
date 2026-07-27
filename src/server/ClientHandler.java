package server;

import model.Card;
import model.CardColor;
import model.GameState;
import model.PlayerState;
import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

public final class ClientHandler implements Runnable {

    private final Socket socket;
    private final int playerId;
    private final String playerName;
    private final PrintWriter out;
    private final BufferedReader in;
    private final GameServer server;

    private volatile GameState game;
    private volatile PlayerState playerState;

    public ClientHandler(Socket socket, int playerId, String playerName,
                         PrintWriter out, BufferedReader in, GameServer server) {
        this.socket = Objects.requireNonNull(socket);
        this.playerId = playerId;
        this.playerName = Objects.requireNonNull(playerName);
        this.out = Objects.requireNonNull(out);
        this.in = Objects.requireNonNull(in);
        this.server = Objects.requireNonNull(server);
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setGame(GameState game) {
        this.game = Objects.requireNonNull(game);
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = Objects.requireNonNull(playerState);
    }

    @Override
    public void run() {
        try {
            out.println("BEM_VINDO " + playerId);
            sendLine("NOME_JOGADOR " + playerName);
            sendState();

            while (!game.isGameOver()) {
                playerState.waitForTurn();
                if (game.isGameOver()) {
                    break;
                }
                sendLine("SUA_VEZ");
                sendState();
                String command = in.readLine();
                if (command == null) {
                    break;
                }
                processCommand(command.trim());
            }

            if (game.isGameOver()) {
                sendLine("FIM_JOGO " + game.getWinner().getPlayerName());
            }
        } catch (IOException e) {
            System.err.println("Erro de comunicação com jogador " + playerName + ": " + e.getMessage());
        } finally {
            closeQuietly();
        }
    }

    private void processCommand(String commandLine) {
        try {
            if (commandLine.equalsIgnoreCase("COMPRAR")) {
                List<Card> drawn = game.drawCards(playerId, 1);
                sendLine("COMPROU " + Protocol.formatHand(drawn));
                server.broadcast("ATUALIZACAO " + playerName + " comprou uma carta.");
            } else if (commandLine.toUpperCase().startsWith("JOGAR ")) {
                String[] tokens = commandLine.split(" ");
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Comando JOGAR inválido. Use JOGAR <COR>:<VALOR> [COR_DECLARADA].");
                }
                Card card = Protocol.parseCard(tokens[1]);
                CardColor declaredColor = null;
                if (tokens.length == 3) {
                    declaredColor = Protocol.parseColor(tokens[2]);
                }
                game.playCard(playerId, card, declaredColor);
                server.broadcast("ATUALIZACAO " + playerName + " jogou " + Protocol.formatCard(card) + ".");
            } else {
                throw new IllegalArgumentException("Comando desconhecido: " + commandLine);
            }
            sendLine("TUDO_BEM");
            sendState();
        } catch (Exception e) {
            sendLine("ERRO " + e.getMessage());
        }
    }

    private void sendState() {
        if (game == null || playerState == null) {
            return;
        }
        sendLine("TOPO " + Protocol.formatCard(game.getTopCard()));
        sendLine("ATIVA " + game.getActiveColor());
        sendLine("ATUAL " + game.currentPlayer().getPlayerName());
        sendLine("MAO " + Protocol.formatHand(playerState.getHandSnapshot()));
    }

    public void sendLine(String message) {
        out.println(message);
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
