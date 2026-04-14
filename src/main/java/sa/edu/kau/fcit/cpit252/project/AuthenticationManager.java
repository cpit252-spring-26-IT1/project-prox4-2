package sa.edu.kau.fcit.cpit252.project;

public class AuthenticationManager {
   
    private static AuthenticationManager instance;
    
    
    private AuthenticationManager() {
        System.out.println("[System] Authentication Manager Initialized for first time.");
    }

   
    public static AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager(); 
        }
        return instance; 
    }

    public void login(User user) {
        System.out.println("User '" + user.getUsername() + "' ["+ user.getRole() +"] has successfully logged in via The Authentication Manager.");
    }
}