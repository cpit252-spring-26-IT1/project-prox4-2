package sa.edu.kau.fcit.cpit252.project.ui;
public class Banner {

    public static void printBanner() {
        System.out.println(Colors.RED + "╔══════════════════════════════════════════════════╗" + Colors.RESET);
        System.out.println(Colors.RED + "║" + Colors.WHITE + "          ProX4 SECURE FILE SYSTEM               " + Colors.RED + "║" + Colors.RESET);
        System.out.println(Colors.RED + "║" + Colors.CYAN  + "              Version 4.0 — CPIT252              " + Colors.RED + "║" + Colors.RESET);
        System.out.println(Colors.RED + "║" + Colors.YELLOW + "       ⚠  Unauthorized Access Will Be Logged ⚠   " + Colors.RED + "║" + Colors.RESET);
        System.out.println(Colors.RED + "╚══════════════════════════════════════════════════╝" + Colors.RESET);
        System.out.println();
    }

    public static void printFirstRunBanner() {
        System.out.println(Colors.YELLOW + "╔══════════════════════════════════════════════════╗" + Colors.RESET);
        System.out.println(Colors.YELLOW + "║" + Colors.WHITE + "           FIRST RUN — SYSTEM SETUP              " + Colors.YELLOW + "║" + Colors.RESET);
        System.out.println(Colors.YELLOW + "║" + Colors.CYAN  + "     Create the OWNER account to get started     " + Colors.YELLOW + "║" + Colors.RESET);
        System.out.println(Colors.YELLOW + "╚══════════════════════════════════════════════════╝" + Colors.RESET);
        System.out.println();
    }

    public static void printSection(String title) {
        System.out.println(Colors.CYAN + "─── " + title + " ───" + Colors.RESET);
    }
}