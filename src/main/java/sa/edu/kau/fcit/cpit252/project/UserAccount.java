package sa.edu.kau.fcit.cpit252.project;

import java.io.Serializable;
import java.time.LocalDateTime;

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
}