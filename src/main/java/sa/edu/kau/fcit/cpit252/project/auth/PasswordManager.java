package sa.edu.kau.fcit.cpit252.project.auth;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordManager {

    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String input, String storedHash) {
        return hash(input).equals(storedHash);
    }
}