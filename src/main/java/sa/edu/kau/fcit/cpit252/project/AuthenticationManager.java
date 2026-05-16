package sa.edu.kau.fcit.cpit252.project;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {
    private static AuthenticationManager instance;
    private final Map<String, Role> userDatabase;

    private AuthenticationManager() {
        userDatabase = new HashMap<>();
        userDatabase.put("Nawaf", Role.MANAGER);
        userDatabase.put("Khaled", Role.MANAGER);
        userDatabase.put("Abdulrahman", Role.MANAGER);
        userDatabase.put("Faisal", Role.USER);
        System.out.println(">> [SYSTEM] Authentication Manager has been successfully initialized.");
    }

    public static synchronized AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }

    public User authenticate(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.out.println(">> [LOGIN FAILED] Username cannot be empty.");
            return null;
        }
        
        String trimmedUsername = username.trim();
        
        // Case-insensitive lookup
        for (Map.Entry<String, Role> entry : userDatabase.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(trimmedUsername)) {
                Role role = entry.getValue();
                System.out.println(">> [LOGIN SUCCESS] Welcome back, " + entry.getKey() + "! Your role is: " + role);
                return new User(entry.getKey(), role);
            }
        }
       
        System.out.println(">> [VISITOR ACCESS] User '" + trimmedUsername + "' not found in our records.");
        System.out.println(">> [SYSTEM] You are now logged in as a GUEST with limited access.");
        return new User(trimmedUsername, Role.GUEST);
    }
}