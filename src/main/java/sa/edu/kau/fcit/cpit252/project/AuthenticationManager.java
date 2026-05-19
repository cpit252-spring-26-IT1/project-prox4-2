package sa.edu.kau.fcit.cpit252.project;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {

    // ── Inner class ──────────────────────────────────────────
    public static class UserAccount {
        private String username;
        private String hashedPassword;
        private Role role;
        private int failedAttempts;
        private LocalDateTime lockoutUntil;

        public UserAccount(String username, String password, Role role) {
            this.username = username;
            this.hashedPassword = PasswordManager.hash(password);
            this.role = role;
            this.failedAttempts = 0;
            this.lockoutUntil = null;
        }

        public String getUsername() { return username; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public String getHashedPassword() { return hashedPassword; }
        public void setHashedPassword(String hash) { this.hashedPassword = hash; }

        public boolean isLocked() {
            return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
        }

        public void incrementFailedAttempts() {
            failedAttempts++;
            if (failedAttempts >= 3 && role != Role.OWNER) {
                lockoutUntil = LocalDateTime.now().plusMinutes(5);
            }
        }

        public void resetFailedAttempts() {
            failedAttempts = 0;
            lockoutUntil = null;
        }

        public LocalDateTime getLockoutUntil() { return lockoutUntil; }
        public int getFailedAttempts() { return failedAttempts; }
    }

    // ── Singleton ─────────────────────────────────────────────
    private static AuthenticationManager instance;
    private final Map<String, UserAccount> accounts;

    private AuthenticationManager() {
        accounts = new HashMap<>();
        accounts.put("Nawaf",        new UserAccount("Nawaf",        "owner123", Role.OWNER));
        accounts.put("Khaled",       new UserAccount("Khaled",       "mgr123",   Role.MANAGER));
        accounts.put("Abdulrahman",  new UserAccount("Abdulrahman",  "mgr123",   Role.MANAGER));
        accounts.put("Faisal",       new UserAccount("Faisal",       "user123",  Role.USER));
        System.out.println(">> [SYSTEM] Authentication Manager initialized.");
    }

    public static synchronized AuthenticationManager getInstance() {
        if (instance == null) instance = new AuthenticationManager();
        return instance;
    }

    public Map<String, UserAccount> getAccounts() { return accounts; }
}