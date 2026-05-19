package sa.edu.kau.fcit.cpit252.project.auth;

import sa.edu.kau.fcit.cpit252.project.model.Role;
import sa.edu.kau.fcit.cpit252.project.database.DatabaseManager;
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



    public boolean addUser(String username, String password, Role role, UserAccount requester) {
        if (findAccount(username) != null) {
            System.out.println(">> [ERROR] Username already exists.");
            return false;
        }
        if (requester.getRole() == Role.MANAGER && role != Role.USER) {
            System.out.println(">> [ERROR] MANAGER can only add USER accounts.");
            return false;
        }
        if (requester.getRole() == Role.MANAGER && role == Role.OWNER) {
            System.out.println(">> [ERROR] Cannot create OWNER account.");
            return false;
        }
        UserAccount newAccount = new UserAccount(username, password, role);
        newAccount.setMustChangePassword(true);
        accounts.put(username, newAccount);
        DatabaseManager.save(accounts);
        System.out.println(">> [SUCCESS] User '" + username + "' created with role: " + role);
        return true;
    }

    public boolean removeUser(String username, UserAccount requester) {
        UserAccount target = findAccount(username);
        if (target == null) {
            System.out.println(">> [ERROR] User not found.");
            return false;
        }
        if (target.getRole() == Role.OWNER) {
            System.out.println(">> [ERROR] Cannot delete OWNER account.");
            return false;
        }
        if (requester.getRole() == Role.MANAGER) {
            System.out.println(">> [ERROR] MANAGER cannot delete users.");
            return false;
        }
        accounts.remove(target.getUsername());
        DatabaseManager.save(accounts);
        System.out.println(">> [SUCCESS] User '" + username + "' deleted.");
        return true;
    }

    public boolean promoteUser(String username, UserAccount requester) {
        UserAccount target = findAccount(username);
        if (target == null) {
            System.out.println(">> [ERROR] User not found.");
            return false;
        }
        if (requester.getRole() != Role.OWNER) {
            System.out.println(">> [ERROR] Only OWNER can promote users.");
            return false;
        }
        if (target.getRole() == Role.OWNER) {
            System.out.println(">> [ERROR] Cannot promote OWNER.");
            return false;
        }
        if (target.getRole() == Role.MANAGER) {
            System.out.println(">> [ERROR] Already at maximum rank (MANAGER).");
            return false;
        }
        Role oldRole = target.getRole();
        target.setRole(oldRole == Role.GUEST ? Role.USER : Role.MANAGER);
        DatabaseManager.save(accounts);
        System.out.println(">> [SUCCESS] '" + username + "' promoted from " + oldRole + " to " + target.getRole());
        return true;
    }

    public boolean demoteUser(String username, UserAccount requester) {
        UserAccount target = findAccount(username);
        if (target == null) {
            System.out.println(">> [ERROR] User not found.");
            return false;
        }
        if (requester.getRole() != Role.OWNER) {
            System.out.println(">> [ERROR] Only OWNER can demote users.");
            return false;
        }
        if (target.getRole() == Role.OWNER) {
            System.out.println(">> [ERROR] Cannot demote OWNER.");
            return false;
        }
        if (target.getRole() == Role.GUEST) {
            System.out.println(">> [ERROR] Already at minimum rank (GUEST).");
            return false;
        }
        Role oldRole = target.getRole();
        target.setRole(oldRole == Role.MANAGER ? Role.USER : Role.GUEST);
        DatabaseManager.save(accounts);
        System.out.println(">> [SUCCESS] '" + username + "' demoted from " + oldRole + " to " + target.getRole());
        return true;
    }
}