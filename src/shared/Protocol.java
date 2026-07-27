package shared;

import model.Card;
import model.CardColor;
import model.CardValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class Protocol {

    private static final Map<String, CardColor> COLOR_ALIASES = new HashMap<>();
    private static final Map<String, CardValue> VALUE_ALIASES = new HashMap<>();
    private static final Map<CardColor, String> COLOR_NAMES_PT = new HashMap<>();
    private static final Map<CardValue, String> VALUE_NAMES_PT = new HashMap<>();

    static {
        COLOR_ALIASES.put("RED", CardColor.RED);
        COLOR_ALIASES.put("VERMELHO", CardColor.RED);
        COLOR_ALIASES.put("BLUE", CardColor.BLUE);
        COLOR_ALIASES.put("AZUL", CardColor.BLUE);
        COLOR_ALIASES.put("GREEN", CardColor.GREEN);
        COLOR_ALIASES.put("VERDE", CardColor.GREEN);
        COLOR_ALIASES.put("YELLOW", CardColor.YELLOW);
        COLOR_ALIASES.put("AMARELO", CardColor.YELLOW);
        COLOR_ALIASES.put("BLACK", CardColor.BLACK);
        COLOR_ALIASES.put("PRETO", CardColor.BLACK);

        VALUE_ALIASES.put("ZERO", CardValue.ZERO);
        VALUE_ALIASES.put("0", CardValue.ZERO);
        VALUE_ALIASES.put("UM", CardValue.ONE);
        VALUE_ALIASES.put("ONE", CardValue.ONE);
        VALUE_ALIASES.put("1", CardValue.ONE);
        VALUE_ALIASES.put("DOIS", CardValue.TWO);
        VALUE_ALIASES.put("TWO", CardValue.TWO);
        VALUE_ALIASES.put("2", CardValue.TWO);
        VALUE_ALIASES.put("TRES", CardValue.THREE);
        VALUE_ALIASES.put("THREE", CardValue.THREE);
        VALUE_ALIASES.put("3", CardValue.THREE);
        VALUE_ALIASES.put("QUATRO", CardValue.FOUR);
        VALUE_ALIASES.put("FOUR", CardValue.FOUR);
        VALUE_ALIASES.put("4", CardValue.FOUR);
        VALUE_ALIASES.put("CINCO", CardValue.FIVE);
        VALUE_ALIASES.put("FIVE", CardValue.FIVE);
        VALUE_ALIASES.put("5", CardValue.FIVE);
        VALUE_ALIASES.put("SEIS", CardValue.SIX);
        VALUE_ALIASES.put("SIX", CardValue.SIX);
        VALUE_ALIASES.put("6", CardValue.SIX);
        VALUE_ALIASES.put("SETE", CardValue.SEVEN);
        VALUE_ALIASES.put("SEVEN", CardValue.SEVEN);
        VALUE_ALIASES.put("7", CardValue.SEVEN);
        VALUE_ALIASES.put("OITO", CardValue.EIGHT);
        VALUE_ALIASES.put("EIGHT", CardValue.EIGHT);
        VALUE_ALIASES.put("8", CardValue.EIGHT);
        VALUE_ALIASES.put("NOVE", CardValue.NINE);
        VALUE_ALIASES.put("NINE", CardValue.NINE);
        VALUE_ALIASES.put("9", CardValue.NINE);
        VALUE_ALIASES.put("PULA", CardValue.SKIP);
        VALUE_ALIASES.put("SKIP", CardValue.SKIP);
        VALUE_ALIASES.put("INVERTE", CardValue.REVERSE);
        VALUE_ALIASES.put("REVERSE", CardValue.REVERSE);
        VALUE_ALIASES.put("COMPRA_DOIS", CardValue.DRAW_TWO);
        VALUE_ALIASES.put("DRAW_TWO", CardValue.DRAW_TWO);
        VALUE_ALIASES.put("WILD", CardValue.WILD);
        VALUE_ALIASES.put("CORINGA", CardValue.WILD);
        VALUE_ALIASES.put("WILD_DRAW_FOUR", CardValue.WILD_DRAW_FOUR);
        VALUE_ALIASES.put("CORINGA_COMPRA_QUATRO", CardValue.WILD_DRAW_FOUR);

        COLOR_NAMES_PT.put(CardColor.RED, "VERMELHO");
        COLOR_NAMES_PT.put(CardColor.BLUE, "AZUL");
        COLOR_NAMES_PT.put(CardColor.GREEN, "VERDE");
        COLOR_NAMES_PT.put(CardColor.YELLOW, "AMARELO");
        COLOR_NAMES_PT.put(CardColor.BLACK, "PRETO");

        VALUE_NAMES_PT.put(CardValue.ZERO, "ZERO");
        VALUE_NAMES_PT.put(CardValue.ONE, "UM");
        VALUE_NAMES_PT.put(CardValue.TWO, "DOIS");
        VALUE_NAMES_PT.put(CardValue.THREE, "TRES");
        VALUE_NAMES_PT.put(CardValue.FOUR, "QUATRO");
        VALUE_NAMES_PT.put(CardValue.FIVE, "CINCO");
        VALUE_NAMES_PT.put(CardValue.SIX, "SEIS");
        VALUE_NAMES_PT.put(CardValue.SEVEN, "SETE");
        VALUE_NAMES_PT.put(CardValue.EIGHT, "OITO");
        VALUE_NAMES_PT.put(CardValue.NINE, "NOVE");
        VALUE_NAMES_PT.put(CardValue.SKIP, "PULA");
        VALUE_NAMES_PT.put(CardValue.REVERSE, "INVERTE");
        VALUE_NAMES_PT.put(CardValue.DRAW_TWO, "COMPRA_DOIS");
        VALUE_NAMES_PT.put(CardValue.WILD, "CORINGA");
        VALUE_NAMES_PT.put(CardValue.WILD_DRAW_FOUR, "CORINGA_COMPRA_QUATRO");
    }

    private Protocol() {
        // utilitário estático
    }

    public static String formatCard(Card card) {
        Objects.requireNonNull(card, "card não pode ser nulo.");
        return COLOR_NAMES_PT.get(card.getColor()) + ":" + VALUE_NAMES_PT.get(card.getValue());
    }

    public static Card parseCard(String token) {
        Objects.requireNonNull(token, "token não pode ser nulo.");
        String[] parts = token.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Token de carta inválido: " + token);
        }
        CardColor color = parseColor(parts[0]);
        CardValue value = parseValue(parts[1]);
        return new Card(color, value);
    }

    public static CardColor parseColor(String value) {
        Objects.requireNonNull(value, "Cor não pode ser nula.");
        CardColor color = COLOR_ALIASES.get(value.toUpperCase());
        if (color == null) {
            throw new IllegalArgumentException("Cor inválida: " + value);
        }
        return color;
    }

    public static CardValue parseValue(String value) {
        Objects.requireNonNull(value, "Valor não pode ser nulo.");
        CardValue cardValue = VALUE_ALIASES.get(value.toUpperCase());
        if (cardValue == null) {
            throw new IllegalArgumentException("Valor inválido: " + value);
        }
        return cardValue;
    }

    public static String formatHand(List<Card> hand) {
        Objects.requireNonNull(hand, "hand não pode ser nulo.");
        StringJoiner joiner = new StringJoiner(",");
        for (Card card : hand) {
            joiner.add(formatCard(card));
        }
        return joiner.toString();
    }

    public static List<Card> parseHand(String payload) {
        Objects.requireNonNull(payload, "payload não pode ser nulo.");
        List<Card> parsed = new ArrayList<>();
        if (payload.isEmpty()) {
            return parsed;
        }
        for (String token : payload.split(",")) {
            parsed.add(parseCard(token));
        }
        return parsed;
    }
}
