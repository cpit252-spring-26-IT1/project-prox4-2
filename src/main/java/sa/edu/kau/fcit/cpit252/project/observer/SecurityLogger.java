package sa.edu.kau.fcit.cpit252.project.observer;

import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;
import sa.edu.kau.fcit.cpit252.project.files.FileResource;
import sa.edu.kau.fcit.cpit252.project.ui.Colors;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SecurityLogger implements AccessObserver {

    private static final String LOG_FILE = "access_log.txt";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onAccessEvent(AccessEvent event, UserAccount user, FileResource file) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String username = user != null ? user.getUsername() : "SYSTEM";
        String role = user != null ? user.getRole().toString() : "SYSTEM";
        String fileName = file != null ? file.getName() : "N/A";

        String logEntry = String.format("[%s] EVENT=%-20s | USER=%-20s | ROLE=%-10s | FILE=%s",
                timestamp, event.name(), username, role, fileName);

        System.out.println(Colors.cyan(">> [LOG] ") + logEntry);

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            System.out.println(Colors.red(">> [LOG ERROR] Could not write to log file: " + e.getMessage()));
        }
    }
}