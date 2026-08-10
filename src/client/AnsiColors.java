package client;

/**
 * Define os códigos ANSI usados para colorir mensagens no terminal.
 * Essas constantes ajudam a deixar a interface textual mais legível em aula.
 */
public final class AnsiColors {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";

    private AnsiColors() {
    }
}
