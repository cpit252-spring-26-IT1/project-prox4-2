package sa.edu.kau.fcit.cpit252.project;

public class User {
    private String username;
    private Role role;

    public User(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public Role getRole() { return role; }

    public boolean canRead(FileResource file) {
        switch (file.getType()) {
            case SENSITIVE:
                return this.role == Role.MANAGER; 
            case INTERNAL:
                return this.role == Role.MANAGER || this.role == Role.USER; 
            case NORMAL:
                return true; 
            default:
                return false;
        }
    }
}