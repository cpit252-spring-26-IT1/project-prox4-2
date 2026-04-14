package sa.edu.kau.fcit.cpit252.project;

//import javax.management.relation.Role;
import java.util.ArrayList;
public class User {
    private String username;
    private Role role; // Uses our custom Enum

    public User(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }


    public boolean canRead(FileResource file) {
        if (file.getType() == FileType.SENSITIVE) {
            return this.role == Role.MANAGER;
        }
        if (file.getType() == FileType.NORMAL) {
            return this.role == Role.MANAGER || this.role == Role.USER;
        }
        return false;
    }

    public boolean canWrite() {
        return this.role == Role.MANAGER;
    }

    public boolean canDelete() {
        return this.role == Role.MANAGER;
    }

    public void displayPermissions(FileResource file) {
        System.out.println("User: " + username);
        System.out.println("Role: " + role.name());
        System.out.println("File Type: " + file.getType().name());
        System.out.println("Can Read: " + canRead(file));
        System.out.println("Can Write: " + canWrite());
        System.out.println("Can Delete: " + canDelete());
        System.out.println("-----------------------");
    }
}


