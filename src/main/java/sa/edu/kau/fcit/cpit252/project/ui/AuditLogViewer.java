package sa.edu.kau.fcit.cpit252.project.ui;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AuditLogViewer {

    private static final String LOG_FILE = "access_log.txt";

    public static void viewLog(int lastN) {
        Path logPath = Paths.get(LOG_FILE);
        if (!Files.exists(logPath)) {
            System.out.println(Colors.yellow(">> [INFO] No audit log found."));
            return;
        }

        try {
            List<String> lines = Files.readAllLines(logPath);
            if (lines.isEmpty()) {
                System.out.println(Colors.yellow(">> [INFO] Audit log is empty."));
                return;
            }

            System.out.println(Colors.cyan("\n─── Audit Log (last " + lastN + " entries) ───"));
            int start = Math.max(0, lines.size() - lastN);
            for (int i = start; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("DENIED")  line.contains("LOCKED")  line.contains("FAILED")) {
                    System.out.println(Colors.red("  " + line));
                } else if (line.contains("GRANTED")  line.contains("SUCCESS")  line.contains("CREATED")) {
                    System.out.println(Colors.green("  " + line));
                } else {
                    System.out.println(Colors.yellow("  " + line));
                }
            }
            System.out.println(Colors.cyan("─────────────────────────────────────────────\n"));

        } catch (IOException e) {
            System.out.println(Colors.red(">> [ERROR] Could not read log: " + e.getMessage()));
        }
    }

    public static void clearLog() {
        try {
            new PrintWriter(LOG_FILE).close();
            System.out.println(Colors.green(">> [SUCCESS] Audit log cleared."));
        } catch (IOException e) {
            System.out.println(Colors.red(">> [ERROR] Could not clear log: " + e.getMessage()));
        }
    }
}