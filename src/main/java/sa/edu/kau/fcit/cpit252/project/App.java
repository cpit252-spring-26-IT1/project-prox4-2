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
import sa.edu.kau.fcit.cpit252.project.model.Operation;

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

    private static boolean handleOwner(String choice, UserAccount user) {
        UserManager um = new UserManager(auth, sc);
        switch (choice) {
            case "1": browseFiles(user); break;
            case "2":
                FileResource f2 = selectFile(user);
                if (f2 != null) proxy.execute(Operation.OPEN, f2, user);
                break;
            case "3":
                FileResource f3 = selectFile(user);
                if (f3 != null) proxy.execute(Operation.MOVE, f3, user);
                break;
            case "4": registerFile(user); break;
            case "5":
                FileResource f5 = selectFile(user);
                if (f5 != null) {
                    proxy.execute(Operation.DELETE, f5, user);
                    registry.delete(f5.getName());
                }
                break;
            case "6":
                FileResource f6 = selectFile(user);
                if (f6 != null) {
                    System.out.println("[1] Lock  [2] Unlock");
                    System.out.print(Colors.white("Choice: "));
                    String lChoice = sc.nextLine().trim();
                    if (lChoice.equals("1")) FileLockManager.lockFile(f6, user);
                    else if (lChoice.equals("2")) FileLockManager.unlockFile(f6, user);
                }
                break;
            case "7":
                System.out.println("[1] Add User  [2] Remove User  [3] Promote  [4] Demote  [5] List Users");
                System.out.print(Colors.white("Choice: "));
                String uChoice = sc.nextLine().trim();
                switch (uChoice) {
                    case "1": um.addUser(user); break;
                    case "2": um.removeUser(user); break;
                    case "3": um.promoteUser(user); break;
                    case "4": um.demoteUser(user); break;
                    case "5": um.listUsers(); break;
                    default: System.out.println(Colors.red(">> [INVALID] Unknown option."));
                }
                break;
            case "8": AuditLogViewer.viewLog(20); break;
            case "9": AuditLogViewer.clearLog(); break;
            case "10":
                promotionManager.showPendingRequests();
                if (!promotionManager.getPendingRequests().isEmpty()) {
                    System.out.print("Approve(A) or Reject(R) request number: ");
                    String ar = sc.nextLine().trim();
                    System.out.print("Request number: ");
                    try {
                        int idx = Integer.parseInt(sc.nextLine().trim());
                        if (ar.equalsIgnoreCase("A")) promotionManager.approveRequest(idx, user, auth);
                        else if (ar.equalsIgnoreCase("R")) promotionManager.rejectRequest(idx);
                    } catch (NumberFormatException e) {
                        System.out.println(Colors.red(">> [INVALID] Invalid number."));
                    }
                }
                break;
            case "11": return false;
            default: System.out.println(Colors.red(">> [INVALID] Unknown option."));
        }
        return true;
    }
    private static boolean handleManager(String choice, UserAccount user) {
        UserManager um = new UserManager(auth, sc);
        switch (choice) {
            case "1": browseFiles(user); break;
            case "2":
                FileResource f2 = selectFile(user);
                if (f2 != null) proxy.execute(Operation.OPEN, f2, user);
                break;
            case "3":
                FileResource f3 = selectFile(user);
                if (f3 != null) proxy.execute(Operation.MOVE, f3, user);
                break;
            case "4": registerFile(user); break;
            case "5":
                FileResource f5 = selectFile(user);
                if (f5 != null) {
                    proxy.execute(Operation.DELETE, f5, user);
                    registry.delete(f5.getName());
                }
                break;
            case "6":
                FileResource f6 = selectFile(user);
                if (f6 != null) {
                    System.out.println("[1] Lock  [2] Unlock");
                    System.out.print(Colors.white("Choice: "));
                    String lChoice = sc.nextLine().trim();
                    if (lChoice.equals("1")) FileLockManager.lockFile(f6, user);
                    else if (lChoice.equals("2")) FileLockManager.unlockFile(f6, user);
                }
                break;
            case "7": um.addUser(user); break;
            case "8": AuditLogViewer.viewLog(20); break;
            case "9": return false;
            default: System.out.println(Colors.red(">> [INVALID] Unknown option."));
        }
        return true;
    }

    private static boolean handleUser(String choice, UserAccount user) {
        switch (choice) {
            case "1": browseFiles(user); break;
            case "2":
                FileResource f2 = selectFile(user);
                if (f2 != null) proxy.execute(Operation.OPEN, f2, user);
                break;
            case "3":
                FileResource f3 = selectFile(user);
                if (f3 != null) proxy.execute(Operation.MOVE, f3, user);
                break;
            case "4": promotionManager.requestPromotion(user); break;
            case "5": return false;
            default: System.out.println(Colors.red(">> [INVALID] Unknown option."));
        }
        return true;
    }

    private static boolean handleGuest(String choice, UserAccount user) {
        switch (choice) {
            case "1": browseFiles(user); break;
            case "2": promotionManager.requestPromotion(user); break;
            case "3": return false;
            default: System.out.println(Colors.red(">> [INVALID] Unknown option."));
        }
        return true;
    }

}