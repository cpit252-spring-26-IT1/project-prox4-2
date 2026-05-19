package sa.edu.kau.fcit.cpit252.project.ui;

import sa.edu.kau.fcit.cpit252.project.auth.AuthenticationManager;
import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;
import sa.edu.kau.fcit.cpit252.project.files.FileRegistry;
import sa.edu.kau.fcit.cpit252.project.files.FileResource;
import sa.edu.kau.fcit.cpit252.project.model.PromotionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class OwnerDashboard {

    public static void show(AuthenticationManager auth) {
        System.out.println(Colors.cyan("\n╔══════════════════════════════════════════════════╗"));
        System.out.println(Colors.cyan("║") + Colors.white("              OWNER DASHBOARD                     ") + Colors.cyan("║"));
        System.out.println(Colors.cyan("╚══════════════════════════════════════════════════╝"));

        // Pending promotion requests
        int pending = PromotionManager.getInstance().getPendingRequests().size();
        System.out.println(Colors.yellow("  >> Pending Promotion Requests: ") + Colors.white(String.valueOf(pending)));

        // Locked files
        int locked = 0;
        for (FileResource file : FileRegistry.getInstance().getAll().values()) {
            if (file.isLocked()) locked++;
        }
        System.out.println(Colors.yellow("  >> Locked Files: ") + Colors.white(String.valueOf(locked)));

        // Total users
        int users = auth.getAccounts().size();
        System.out.println(Colors.yellow("  >> Registered Users: ") + Colors.white(String.valueOf(users)));

        // Recent denied events
        int denied = 0;
        try {
            List<String> lines = Files.readAllLines(Paths.get("access_log.txt"));
            for (String line : lines) {
                if (line.contains("ACCESS_DENIED") || line.contains("LOGIN_FAILED")) denied++;
            }
        } catch (IOException e) {
            denied = 0;
        }
        System.out.println(Colors.yellow("  >> Total Security Violations Logged: ") + Colors.red(String.valueOf(denied)));
        System.out.println(Colors.cyan("──────────────────────────────────────────────────\n"));
    }
}