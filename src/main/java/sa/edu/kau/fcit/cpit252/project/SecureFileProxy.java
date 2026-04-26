package sa.edu.kau.fcit.cpit252.project;
import java.util.HashMap;
import java.util.Map;

public class SecureFileProxy implements FileAccess {
    private RealFileAccess realService;
    private Map<String, Integer> tracker = new HashMap<>();

    public SecureFileProxy() { this.realService = new RealFileAccess(); }

    @Override
    public void openFile(FileResource file, User user) {
        System.out.println("++ [PROXY] Checking security protocols for: " + file.getName());
        
        if (!user.canRead(file)) {
            System.out.println("XX [PROXY DENIED] Security Alert: Role [" + user.getRole() + "] is unauthorized to view " + file.getType() + " files.");
            return;
        }

        String key = user.getUsername() + ":" + file.getName();
        int views = tracker.getOrDefault(key, 0);

        if (views >= 3) {
            System.out.println("XX [PROXY BLOCKED] Limit Reached: You have already viewed this file 3 times.");
        } else {
            tracker.put(key, views + 1);
            System.out.println("++ [PROXY GRANTED] Permission verified. (View Count: " + (views + 1) + "/3)");
            realService.openFile(file, user);
        }
    }
}