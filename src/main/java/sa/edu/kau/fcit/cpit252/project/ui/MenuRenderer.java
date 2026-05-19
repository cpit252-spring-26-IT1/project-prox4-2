package sa.edu.kau.fcit.cpit252.project.ui;

import sa.edu.kau.fcit.cpit252.project.model.Role;

public class MenuRenderer {

    public static void showMenu(Role role) {
        System.out.println(Colors.cyan("\n--- USER MENU [" + role + "] ---"));

        switch (role) {
            case OWNER:
                showOwnerMenu();
                break;
            case MANAGER:
                showManagerMenu();
                break;
            case USER:
                showUserMenu();
                break;
            case GUEST:
                showGuestMenu();
                break;
        }
    }

    private static void showOwnerMenu() {
        System.out.println(Colors.white("1.") + " Browse Files");
        System.out.println(Colors.white("2.") + " Open File");
        System.out.println(Colors.white("3.") + " Move File to Workspace");
        System.out.println(Colors.white("4.") + " Register New File");
        System.out.println(Colors.white("5.") + " Delete File");
        System.out.println(Colors.white("6.") + " Lock / Unlock File");
        System.out.println(Colors.white("7.") + " Manage Users");
        System.out.println(Colors.white("8.") + " View Audit Log");
        System.out.println(Colors.white("9.") + " Clear Audit Log");
        System.out.println(Colors.white("10.") + " Pending Promotion Requests");
        System.out.println(Colors.white("11.") + " Logout");
    }

    private static void showManagerMenu() {
        System.out.println(Colors.white("1.") + " Browse Files");
        System.out.println(Colors.white("2.") + " Open File");
        System.out.println(Colors.white("3.") + " Move File to Workspace");
        System.out.println(Colors.white("4.") + " Register New File");
        System.out.println(Colors.white("5.") + " Delete NORMAL Files");
        System.out.println(Colors.white("6.") + " Lock / Unlock File");
        System.out.println(Colors.white("7.") + " Add USER Account");
        System.out.println(Colors.white("8.") + " View Audit Log");
        System.out.println(Colors.white("9.") + " Logout");
    }

    private static void showUserMenu() {
        System.out.println(Colors.white("1.") + " Browse Files");
        System.out.println(Colors.white("2.") + " Open File");
        System.out.println(Colors.white("3.") + " Move File to Workspace");
        System.out.println(Colors.white("4.") + " Request Promotion");
        System.out.println(Colors.white("5.") + " Logout");
    }

    private static void showGuestMenu() {
        System.out.println(Colors.white("1.") + " Browse Files");
        System.out.println(Colors.white("2.") + " Request Promotion");
        System.out.println(Colors.white("3.") + " Logout");
    }
}