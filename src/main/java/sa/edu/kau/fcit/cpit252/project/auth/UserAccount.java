package sa.edu.kau.fcit.cpit252.project.auth;

import java.io.Serializable;
import java.time.LocalDateTime;

import sa.edu.kau.fcit.cpit252.project.model.Role;

public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String hashedPassword;
    private Role role;
    private int failedAttempts;
    private LocalDateTime lockoutUntil;
    private boolean mustChangePassword;

    public UserAccount(String username, String password, Role role) {
        this.username = username;
        this.hashedPassword = PasswordManager.hash(password);
        this.role = role;
        this.failedAttempts = 0;
        this.lockoutUntil = null;
        this.mustChangePassword = false;
    }

    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hash) { this.hashedPassword = hash; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean v) { this.mustChangePassword = v; }
    public int getFailedAttempts() { return failedAttempts; }
    public LocalDateTime getLockoutUntil() { return lockoutUntil; }

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

    
}