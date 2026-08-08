package client;

import java.util.HashMap;
import java.util.Map;

public final class AnsiFormatter {

    private static final Map<String, String> TAGS = new HashMap<>();

    static {
        TAGS.put("bold",    AnsiColors.BOLD);
        TAGS.put("cyan",    AnsiColors.CYAN);
        TAGS.put("yellow",  AnsiColors.YELLOW);
        TAGS.put("green",   AnsiColors.GREEN);
        TAGS.put("red",     AnsiColors.RED);
        TAGS.put("magenta", AnsiColors.MAGENTA); // necessário para [magenta] em FIM_JOGO
        TAGS.put("blue",    AnsiColors.BLUE);    // necessário para [bold blue]
        TAGS.put("reset",   AnsiColors.RESET);
    }

    private AnsiFormatter() {
    }

    public static String parse(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int openStart = text.indexOf('[', index);
            if (openStart == -1) {
                result.append(text.substring(index));
                break;
            }
            result.append(text, index, openStart);
            int openEnd = text.indexOf(']', openStart);
            if (openEnd == -1) {
                result.append(text.substring(openStart));
                break;
            }

            String tag = text.substring(openStart + 1, openEnd).trim();
            if (tag.startsWith("/")) {
                result.append(AnsiColors.RESET);
                index = openEnd + 1;
                continue;
            }

            String[] parts = tag.split("\\s+");
            StringBuilder codes = new StringBuilder();
            for (String part : parts) {
                String code = TAGS.get(part.toLowerCase());
                if (code != null) {
                    codes.append(code);
                }
            }
            if (codes.length() > 0) {
                result.append(codes);
            } else {
                result.append(text, openStart, openEnd + 1);
            }
            index = openEnd + 1;
        }

        result.append(AnsiColors.RESET);
        return result.toString();
    }
}
