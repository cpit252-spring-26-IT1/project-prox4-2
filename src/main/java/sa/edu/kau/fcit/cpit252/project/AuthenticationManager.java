package sa.edu.kau.fcit.cpit252.project;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {
    private static AuthenticationManager instance;
    private Map<String, Role> userDatabase;

    private AuthenticationManager() {
        userDatabase = new HashMap<>();
        userDatabase.put("Nawaf", Role.MANAGER);
        userDatabase.put("Khaled", Role.MANAGER);
        userDatabase.put("Abdulrahman", Role.MANAGER);
        userDatabase.put("Faisal", Role.USER);
        System.out.println(">> [SYSTEM] Authentication Manager has been successfully initialized.");
    }

    public static AuthenticationManager getInstance() {
        if (instance == null) instance = new AuthenticationManager();
        return instance;
    }

    public User authenticate(String username) {
        if (userDatabase.containsKey(username)) {
            Role role = userDatabase.get(username);
            System.out.println(">> [LOGIN SUCCESS] Welcome back, " + username + "! Your role is: " + role);
            return new User(username, role);
        }
       
        System.out.println(">> [VISITOR ACCESS] User '" + username + "' not found in our records.");
        System.out.println(">> [SYSTEM] You are now logged in as a GUEST with limited access.");
        return new User(username, Role.GUEST);
    }
}