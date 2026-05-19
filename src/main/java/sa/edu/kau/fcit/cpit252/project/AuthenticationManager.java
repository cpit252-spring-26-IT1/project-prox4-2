package sa.edu.kau.fcit.cpit252.project;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {

    private static AuthenticationManager instance;
    private Map<String, UserAccount> accounts;

    private AuthenticationManager() {
        Map<String, UserAccount> loaded = DatabaseManager.load();
        if (loaded != null) {
            accounts = loaded;
            System.out.println(">> [SYSTEM] Authentication Manager initialized.");
        } else {
            accounts = new HashMap<>();
            System.out.println(">> [SYSTEM] First run detected. No accounts found.");
        }
    }

    public static synchronized AuthenticationManager getInstance() {
        if (instance == null) instance = new AuthenticationManager();
        return instance;
    }

    public boolean isFirstRun() {
        return accounts.isEmpty();
    }

    public void createOwner(String username, String password) {
        UserAccount owner = new UserAccount(username, password, Role.OWNER);
        accounts.put(username, owner);
        DatabaseManager.save(accounts);
        System.out.println(">> [SYSTEM] OWNER account created successfully.");
    }

    public UserAccount login(String username, String password) {
        UserAccount account = findAccount(username);

        if (account == null) {
            System.out.println(">> [LOGIN FAILED] Account not found.");
            return null;
        }

        if (account.isLocked()) {
            System.out.println(">> [LOGIN FAILED] Account is locked. Try again later.");
            return null;
        }

        if (!PasswordManager.verify(password, account.getHashedPassword())) {
            account.incrementFailedAttempts();
            DatabaseManager.save(accounts);
            int remaining = 3 - account.getFailedAttempts();
            if (account.isLocked()) {
                System.out.println(">> [ACCOUNT LOCKED] Too many failed attempts. Locked for 5 minutes.");
            } else {
                System.out.println(">> [LOGIN FAILED] Wrong password. (" + remaining + " attempts remaining)");
            }
            return null;
        }

        account.resetFailedAttempts();
        DatabaseManager.save(accounts);
        System.out.println(">> [LOGIN SUCCESS] Welcome, " + account.getUsername() + " (" + account.getRole() + ")");
        return account;
    }

    private UserAccount findAccount(String username) {
        for (Map.Entry<String, UserAccount> entry : accounts.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(username)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<String, UserAccount> getAccounts() { return accounts; }
}