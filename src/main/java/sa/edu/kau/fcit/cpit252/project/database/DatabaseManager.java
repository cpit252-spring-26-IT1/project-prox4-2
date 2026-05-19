package sa.edu.kau.fcit.cpit252.project.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import sa.edu.kau.fcit.cpit252.project.auth.CryptoManager;
import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;


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

    @SuppressWarnings("unchecked")
    public static Map<String, UserAccount> load() {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            return null; // First run
        }

        try {
            // Read and decrypt
            String encrypted = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            String decrypted = CryptoManager.decrypt(encrypted);
            byte[] bytes = java.util.Base64.getDecoder().decode(decrypted);

            // Deserialize
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Map<String, UserAccount> accounts = (Map<String, UserAccount>) ois.readObject();
            ois.close();
            return accounts;

        } catch (Exception e) {
            System.out.println(">> [DB ERROR] Could not load data: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static boolean isFirstRun() {
        return !new File(DB_FILE).exists();
    }

    
}