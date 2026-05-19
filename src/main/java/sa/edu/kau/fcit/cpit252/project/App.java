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
}