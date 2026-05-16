package sa.edu.kau.fcit.cpit252.project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SecurityLogger implements AccessObserver {
    
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onAccessEvent(AccessEvent event, User user, FileResource file) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] EVENT=%-15s | USER=%-20s | ROLE=%-10s | FILE=%s",
                timestamp,
                event.name(),
                user.getUsername(),
                user.getRole().name(),
                file.getName());

        System.out.println(">> [LOG] " + logEntry);
    }
}