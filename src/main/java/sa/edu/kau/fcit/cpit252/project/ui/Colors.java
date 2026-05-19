package sa.edu.kau.fcit.cpit252.project.ui;
public class Colors {
    public static final String RESET   = "\033[0m";
    public static final String RED     = "\033[0;31m";
    public static final String GREEN   = "\033[0;32m";
    public static final String YELLOW  = "\033[0;33m";
    public static final String CYAN    = "\033[0;36m";
    public static final String WHITE   = "\033[1;37m";
    public static final String BOLD    = "\033[1m";
    public static final String PURPLE  = "\033[0;35m";

    public static String red(String text)    { return RED    + text + RESET; }
    public static String green(String text)  { return GREEN  + text + RESET; }
    public static String yellow(String text) { return YELLOW + text + RESET; }
    public static String cyan(String text)   { return CYAN   + text + RESET; }
    public static String white(String text)  { return WHITE  + text + RESET; }
    public static String bold(String text)   { return BOLD   + text + RESET; }
    public static String purple(String text) { return PURPLE + text + RESET; }
}