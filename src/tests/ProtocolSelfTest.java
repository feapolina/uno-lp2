package tests;

import model.Card;
import model.CardColor;
import model.CardValue;
import shared.Protocol;

import java.util.List;

public final class ProtocolSelfTest {

    public static void main(String[] args) {
        testBuildAndParseMessage();
        testLegacyWhitespaceFallback();
        testCardAndHandRoundTrip();
        System.out.println("ProtocolSelfTest: OK");
    }

    private static void testBuildAndParseMessage() {
        String wire = Protocol.buildMessage(Protocol.CMD_JOGAR, "VERMELHO:CINCO", "AZUL");
        String[] parsed = Protocol.parseMessage(wire);

        assertEquals(3, parsed.length, "Mensagem tokenizada deve ter 3 partes");
        assertEquals(Protocol.CMD_JOGAR, parsed[0], "Comando inválido");
        assertEquals("VERMELHO:CINCO", parsed[1], "Carta inválida");
        assertEquals("AZUL", parsed[2], "Cor declarada inválida");
    }

    private static void testLegacyWhitespaceFallback() {
        String[] parsed = Protocol.parseMessage("JOGAR VERMELHO:CINCO AZUL");
        assertEquals(3, parsed.length, "Fallback por espaço deve manter compatibilidade");
        assertEquals("JOGAR", parsed[0], "Comando legacy inválido");
    }

    private static void testCardAndHandRoundTrip() {
        Card card = new Card(CardColor.RED, CardValue.FIVE);
        String token = Protocol.formatCard(card);
        Card parsedCard = Protocol.parseCard(token);
        assertEquals(card, parsedCard, "Round-trip de carta falhou");

        List<Card> hand = List.of(
                new Card(CardColor.RED, CardValue.ONE),
                new Card(CardColor.BLUE, CardValue.SKIP),
                new Card(CardColor.BLACK, CardValue.WILD)
        );
        String handToken = Protocol.formatHand(hand);
        List<Card> parsedHand = Protocol.parseHand(handToken);
        assertEquals(hand, parsedHand, "Round-trip de mão falhou");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " | esperado=" + expected + " atual=" + actual);
        }
    }
}
