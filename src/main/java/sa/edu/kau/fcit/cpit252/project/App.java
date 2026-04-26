package sa.edu.kau.fcit.cpit252.project;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AuthenticationManager auth = AuthenticationManager.getInstance();
        FileAccess proxy = new SecureFileProxy();

        System.out.println("==========================================");
        System.out.println("   !WELCOME TO PROXY4 SECURE FILE SYSTEM!   ");
        System.out.println("==========================================");

        while (true) {
            System.out.println("\n--- LOGIN ---");
            System.out.print("Enter Username (or type 'EXIT' to shut down): ");
            String name = sc.nextLine();

            if (name.equalsIgnoreCase("EXIT")) {
                System.out.println(">> [SYSTEM] Shutting down. Thank you for using ProXY4.");
                break;
            }

            User user = auth.authenticate(name);
            boolean session = true;

            while (session) {
                System.out.println("\n--- USER MENU [" + user.getUsername() + "] ---");
                System.out.println("1. Add and Open a Local File");
                System.out.println("2. Switch User / Logout");
                System.out.println("3. Exit System");
                System.out.print("Your Selection: ");
                
                String choice = sc.nextLine();
                switch (choice) {
                    case "1":
                        System.out.println("\n>> [ACTION] Adding a new file resource...");
                        System.out.print("Enter File Display Name: ");
                        String fn = sc.nextLine();
                        System.out.print("Enter Full Disk Path (e.g., C:/files/data.txt): ");
                        String fp = sc.nextLine();
                        System.out.println("Select Privacy Level: [1] SENSITIVE, [2] INTERNAL, [3] NORMAL");
                        System.out.print("Level: ");
                        int t = Integer.parseInt(sc.nextLine());
                        FileType ft = (t==1)? FileType.SENSITIVE : (t==2)? FileType.INTERNAL : FileType.NORMAL;

                        FileResource res = new FileResource(fn, fp, ft);
                        System.out.println(">> [STATUS] File metadata created. Passing to Proxy...");
                        proxy.openFile(res, user);
                        break;

                    case "2":
                        System.out.println(">> [LOGOUT] Session for " + user.getUsername() + " has ended.");
                        session = false;
                        break;

                    case "3":
                        System.out.println(">> [SYSTEM] Closing all connections. Goodbye!");
                        System.exit(0);

                    default:
                        System.out.println(">> [INVALID] Unknown option. Please select 1, 2, or 3.");
                }
            }
        }
        sc.close();
    }
}