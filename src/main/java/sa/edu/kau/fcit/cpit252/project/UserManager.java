package sa.edu.kau.fcit.cpit252.project;

import java.util.Scanner;

public class UserManager {

    private final AuthenticationManager auth;
    private final Scanner sc;

    public UserManager(AuthenticationManager auth, Scanner sc) {
        this.auth = auth;
        this.sc = sc;
    }

    public void addUser(UserAccount requester) {
        System.out.println(Colors.cyan("\n─── Add New User ───"));

        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println(Colors.red(">> [INVALID] Username cannot be empty."));
            return;
        }

        System.out.print("Enter password: ");
        String password = sc.nextLine().trim();
        if (password.isEmpty()) {
            System.out.println(Colors.red(">> [INVALID] Password cannot be empty."));
            return;
        }

        System.out.print("Confirm password: ");
        String confirm = sc.nextLine().trim();
        if (!password.equals(confirm)) {
            System.out.println(Colors.red(">> [INVALID] Passwords do not match."));
            return;
        }

        Role role;
        if (requester.getRole() == Role.OWNER) {
            System.out.println("Select role: [1] MANAGER  [2] USER  [3] GUEST");
            System.out.print("Role: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": role = Role.MANAGER; break;
                case "2": role = Role.USER; break;
                case "3": role = Role.GUEST; break;
                default:
                    System.out.println(Colors.red(">> [INVALID] Unknown role."));
                    return;
            }
        } else {
            role = Role.USER;
            System.out.println(Colors.yellow(">> [INFO] MANAGER can only create USER accounts."));
        }

        auth.addUser(username, password, role, requester);
    }
}