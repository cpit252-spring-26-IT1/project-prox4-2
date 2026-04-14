package sa.edu.kau.fcit.cpit252.project;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        System.out.println("=== Welcome to ProXY4 Secure System ===");

        
        AuthenticationManager authManager1 = AuthenticationManager.getInstance();


        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username to login: ");
        String inputName = scanner.nextLine();

        
        User currentUser = new User(inputName, Role.MANAGER);
        
        
        authManager1.login(currentUser);

        
        System.out.println("\n[System Check] Testing File Permissions...");
        FileResource topSecretFile = new FileResource("Exam_Answers.pdf", FileType.SENSITIVE);
        
        System.out.println("\n--- Permissions for: " + currentUser.getUsername() + " ---");
        currentUser.displayPermissions(topSecretFile);
        
        System.out.println("\nSession ended. Goodbye!");
        scanner.close();
    }
}