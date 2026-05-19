package sa.edu.kau.fcit.cpit252.project;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class DatabaseManager {

    private static final String DB_FILE = "users.dat";

    public static void save(Map<String, UserAccount> accounts) {
        try {
            // Serialize to bytes
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(accounts);
            oos.close();

            // Encrypt and write to file
            String encrypted = CryptoManager.encrypt(
                java.util.Base64.getEncoder().encodeToString(bos.toByteArray())
            );
            try (PrintWriter pw = new PrintWriter(new FileWriter(DB_FILE))) {
                pw.print(encrypted);
            }
            System.out.println(">> [DB] Data saved successfully.");

        } catch (Exception e) {
            System.out.println(">> [DB ERROR] Could not save data: " + e.getMessage());
        }
    }
}