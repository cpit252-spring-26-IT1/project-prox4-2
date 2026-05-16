package sa.edu.kau.fcit.cpit252.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RealFileAccess implements FileAccess {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void openFile(FileResource file, User user) {
        try {
            File f = new File(file.getPath());

            if (!f.exists()) {
                System.out.println(">> [ERROR] The file does not exist at the specified path: " + file.getPath());
                return;
            }

            if (!f.isFile()) {
                System.out.println(">> [ERROR] The specified path is not a file: " + file.getPath());
                return;
            }

            if (!f.canRead()) {
                System.out.println(">> [ERROR] No read permission for file: " + file.getPath());
                return;
            }

            System.out.println(">> [FILE FOUND] Path: " + f.getAbsolutePath());
            System.out.println(">> [OPENING] Reading file content...");

            // Watermark header
            String timestamp = LocalDateTime.now().format(FORMATTER);
            System.out.println("╔══════════════════════════════════════════════╗");
            System.out.println("  CONFIDENTIAL - Viewed by: " + user.getUsername() + " (" + user.getRole() + ")");
            System.out.println("  Date: " + timestamp);
            System.out.println("  File: " + file.getName() + " [" + file.getType() + "]");
            System.out.println("╠══════════════════════════════════════════════╣");

            List<String> lines = Files.readAllLines(f.toPath());
            if (lines.isEmpty()) {
                System.out.println("  (File is empty)");
            } else {
                for (String line : lines) {
                    System.out.println("  " + line);
                }
            }

            // Watermark footer
            System.out.println("╠══════════════════════════════════════════════╣");
            System.out.println("  END OF DOCUMENT - ProX4 Secure File System");
            System.out.println("╚══════════════════════════════════════════════╝");

        } catch (IOException e) {
            System.out.println(">> [SYSTEM ERROR] IO problem while reading file: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println(">> [SYSTEM ERROR] Security restriction: " + e.getMessage());
        }
    }
}