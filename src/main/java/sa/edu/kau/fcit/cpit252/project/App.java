package sa.edu.kau.fcit.cpit252.project;

import sa.edu.kau.fcit.cpit252.project.auth.AuthenticationManager;
import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;
import sa.edu.kau.fcit.cpit252.project.files.FileRegistry;
import sa.edu.kau.fcit.cpit252.project.files.FileResource;
import sa.edu.kau.fcit.cpit252.project.files.FileType;
import sa.edu.kau.fcit.cpit252.project.files.FileLockManager;
import sa.edu.kau.fcit.cpit252.project.model.Operation;
import sa.edu.kau.fcit.cpit252.project.model.PromotionManager;
import sa.edu.kau.fcit.cpit252.project.model.Role;
import sa.edu.kau.fcit.cpit252.project.observer.AccessEvent;
import sa.edu.kau.fcit.cpit252.project.observer.AlertObserver;
import sa.edu.kau.fcit.cpit252.project.observer.SecurityLogger;
import sa.edu.kau.fcit.cpit252.project.proxy.SecureFileProxy;
import sa.edu.kau.fcit.cpit252.project.ui.*;

import java.util.Scanner;

public class App {

    private static Scanner sc = new Scanner(System.in);
    private static AuthenticationManager auth = AuthenticationManager.getInstance();
    private static SecureFileProxy proxy = new SecureFileProxy();
    private static FileRegistry registry = FileRegistry.getInstance();
    private static PromotionManager promotionManager = PromotionManager.getInstance();

    public static void main(String[] args) {
        proxy.addObserver(new SecurityLogger());
        proxy.addObserver(new AlertObserver());

        Banner.printBanner();

        if (auth.isFirstRun()) {
            firstRunSetup();
        }

        loginLoop();
        sc.close();
    }

    private static void firstRunSetup() {
        Banner.printFirstRunBanner();

        String username = "";
        while (username.isEmpty()) {
            System.out.print(Colors.white("Enter OWNER username: "));
            username = sc.nextLine().trim();
            if (username.isEmpty())
                System.out.println(Colors.red(">> [INVALID] Username cannot be empty."));
        }

        String password = "";
        String confirm = " ";
        while (!password.equals(confirm) || password.isEmpty()) {
            System.out.print(Colors.white("Enter OWNER password: "));
            password = sc.nextLine().trim();
            System.out.print(Colors.white("Confirm password:     "));
            confirm = sc.nextLine().trim();
            if (!password.equals(confirm))
                System.out.println(Colors.red(">> [INVALID] Passwords do not match. Try again."));
            if (password.isEmpty())
                System.out.println(Colors.red(">> [INVALID] Password cannot be empty."));
        }

        auth.createOwner(username, password);
        System.out.println(Colors.green("\n>> [SUCCESS] System is ready. Please login.\n"));
    }

    private static void loginLoop() {
        while (true) {
            System.out.println(Colors.cyan("─── LOGIN ───"));
            System.out.println(Colors.white("1.") + " Login");
            System.out.println(Colors.white("2.") + " Continue as Guest");
            System.out.println(Colors.white("3.") + " Exit");
            System.out.print(Colors.white("Your choice: "));

            if (!sc.hasNextLine()) break;
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print(Colors.white("Username: "));
                    String username = sc.nextLine().trim();
                    System.out.print(Colors.white("Password: "));
                    String password = sc.nextLine().trim();

                    UserAccount user = auth.login(username, password);
                    if (user != null) {
                        if (user.getRole() == Role.OWNER) {
                            OwnerDashboard.show(auth);
                        }
                        sessionLoop(user);
                    }
                    break;

                case "2":
                    UserAccount guest = new UserAccount("Guest", "guest", Role.GUEST);
                    System.out.println(Colors.yellow(">> [GUEST] Limited access granted."));
                    sessionLoop(guest);
                    break;

                case "3":
                    System.out.println(Colors.white(">> [SYSTEM] Goodbye!"));
                    return;

                default:
                    System.out.println(Colors.red(">> [INVALID] Please select 1, 2, or 3."));
            }
        }
    }

    private static void sessionLoop(UserAccount user) {
        boolean session = true;
        while (session) {
            MenuRenderer.showMenu(user.getRole());
            System.out.print(Colors.white("Your choice: "));
            if (!sc.hasNextLine()) break;
            String choice = sc.nextLine().trim();

            switch (user.getRole()) {
                case OWNER:   session = handleOwner(choice, user); break;
                case MANAGER: session = handleManager(choice, user); break;
                case USER:    session = handleUser(choice, user); break;
                case GUEST:   session = handleGuest(choice, user); break;
            }
        }
        System.out.println(Colors.yellow(">> [LOGOUT] Session for " + user.getUsername() + " ended."));
    }

    private static void browseFiles(UserAccount user) {
        var files = registry.getAllVisibleTo(user.getRole());
        if (files.isEmpty()) {
            System.out.println(Colors.yellow(">> [INFO] No files available for your role."));
            return;
        }
        System.out.println(Colors.cyan("\n─── File Registry ───"));
        int i = 1;
        for (FileResource file : files.values()) {
            String locked = file.isLocked() ? Colors.red(" [LOCKED]") : "";
            System.out.println(Colors.white("  " + i + ".") + " " + file.getName()
                    + " [" + file.getType() + "]" + locked);
            i++;
        }
        System.out.println();
    }

    private static FileResource selectFile(UserAccount user) {
        browseFiles(user);
        var files = registry.getAllVisibleTo(user.getRole());
        if (files.isEmpty()) return null;

        System.out.print(Colors.white("Enter file name: "));
        String name = sc.nextLine().trim();
        FileResource file = registry.getFile(name);
        if (file == null) {
            System.out.println(Colors.red(">> [ERROR] File not found in registry."));
        }
        return file;
    }

    private static void registerFile(UserAccount user) {
        System.out.print(Colors.white("File display name: "));
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println(Colors.red(">> [INVALID] Name cannot be empty.")); return; }

        System.out.print(Colors.white("Full disk path: "));
        String path = sc.nextLine().trim();
        if (path.isEmpty()) { System.out.println(Colors.red(">> [INVALID] Path cannot be empty.")); return; }

        System.out.println("Select type: [1] SENSITIVE  [2] INTERNAL  [3] NORMAL");
        System.out.print(Colors.white("Type: "));
        String t = sc.nextLine().trim();
        FileType ft;
        switch (t) {
            case "1": ft = FileType.SENSITIVE; break;
            case "2": ft = FileType.INTERNAL; break;
            case "3": ft = FileType.NORMAL; break;
            default: System.out.println(Colors.red(">> [INVALID] Unknown type.")); return;
        }
        registry.register(new FileResource(name, path, ft));
    }
}