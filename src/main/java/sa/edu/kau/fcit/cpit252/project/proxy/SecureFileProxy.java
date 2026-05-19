package sa.edu.kau.fcit.cpit252.project.proxy;

import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;
import sa.edu.kau.fcit.cpit252.project.files.FileResource;
import sa.edu.kau.fcit.cpit252.project.files.FileType;
import sa.edu.kau.fcit.cpit252.project.files.TimeAccessChecker;
import sa.edu.kau.fcit.cpit252.project.model.Role;
import sa.edu.kau.fcit.cpit252.project.observer.AccessEvent;
import sa.edu.kau.fcit.cpit252.project.observer.AccessObserver;
import sa.edu.kau.fcit.cpit252.project.ui.Colors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecureFileProxy implements FileAccess {

    private final RealFileAccess realService;
    private final Map<String, Integer> tracker;
    private final List<AccessObserver> observers;

    public SecureFileProxy() {
        this.realService = new RealFileAccess();
        this.tracker = new HashMap<>();
        this.observers = new ArrayList<>();
    }

    // ── Observer Management ───────────────────────────────────
    public void addObserver(AccessObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AccessObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(AccessEvent event, UserAccount user, FileResource file) {
        for (AccessObserver observer : observers) {
            observer.onAccessEvent(event, user, file);
        }
    }

    // ── Main Entry Point ──────────────────────────────────────
    public void execute(Operation operation, FileResource file, UserAccount user) {
        if (file == null || user == null) {
            System.out.println(Colors.red(">> [PROXY ERROR] Invalid file or user reference."));
            return;
        }

        System.out.println(Colors.cyan("++ [PROXY] Checking security protocols for: " + file.getName()));

        // 1. Time check (OWNER bypasses)
        if (user.getRole() != Role.OWNER) {
            TimeAccessChecker timeChecker = new TimeAccessChecker();
            if (!timeChecker.isAccessAllowed()) {
                System.out.println(Colors.red("XX [PROXY DENIED] Access outside business hours: " + timeChecker.getAccessWindow()));
                notifyObservers(AccessEvent.ACCESS_DENIED, user, file);
                return;
            }
        }

        // 2. Lock check (OWNER bypasses)
        if (file.isLocked() && user.getRole() != Role.OWNER) {
            System.out.println(Colors.red("XX [PROXY DENIED] File is locked by administrator."));
            notifyObservers(AccessEvent.ACCESS_DENIED, user, file);
            return;
        }

        // 3. Permission check
        if (!user.getRole().equals(Role.OWNER) && !canPerform(user, operation, file)) {
            System.out.println(Colors.red("XX [PROXY DENIED] Role [" + user.getRole() + "] cannot perform " + operation + " on " + file.getType() + " files."));
            notifyObservers(AccessEvent.ACCESS_DENIED, user, file);
            return;
        }

        // 4. View limit (OPEN only, OWNER bypasses)
        if (operation == Operation.OPEN && user.getRole() != Role.OWNER) {
            String key = user.getUsername() + ":" + file.getName();
            int views = tracker.getOrDefault(key, 0);
            if (views >= 3) {
                System.out.println(Colors.red("XX [PROXY BLOCKED] Limit Reached: You have already viewed this file 3 times."));
                notifyObservers(AccessEvent.LIMIT_REACHED, user, file);
                return;
            }
            tracker.put(key, views + 1);
            System.out.println(Colors.green("++ [PROXY GRANTED] Permission verified. (View Count: " + (views + 1) + "/3)"));
        } else {
            System.out.println(Colors.green("++ [PROXY GRANTED] Permission verified."));
        }

        notifyObservers(AccessEvent.ACCESS_GRANTED, user, file);

        // 5. Execute operation
        switch (operation) {
            case OPEN:
                realService.openFile(file, user);
                break;
            case MOVE:
                moveFile(file, user);
                break;
            case DELETE:
                System.out.println(Colors.yellow(">> [DELETE] File '" + file.getName() + "' removed from registry."));
                break;
            case LOCK:
                file.setLocked(true);
                System.out.println(Colors.yellow(">> [LOCK] File '" + file.getName() + "' is now locked."));
                break;
            case UNLOCK:
                file.setLocked(false);
                System.out.println(Colors.green(">> [UNLOCK] File '" + file.getName() + "' is now unlocked."));
                break;
            default:
                System.out.println(Colors.yellow(">> [PROXY] Operation noted."));
        }
    }

    private boolean canPerform(UserAccount user, Operation operation, FileResource file) {
        Role role = user.getRole();
        FileType type = file.getType();

        switch (operation) {
            case OPEN:
            case MOVE:
                if (type == FileType.SENSITIVE) return role == Role.MANAGER || role == Role.OWNER;
                if (type == FileType.INTERNAL)  return role == Role.MANAGER || role == Role.USER || role == Role.OWNER;
                if (type == FileType.NORMAL)    return role != Role.GUEST;
                return false;
            case DELETE:
                if (role == Role.OWNER)   return true;
                if (role == Role.MANAGER) return type == FileType.NORMAL;
                return false;
            case LOCK:
            case UNLOCK:
                return role == Role.MANAGER || role == Role.OWNER;
            case REGISTER:
                return role == Role.MANAGER || role == Role.OWNER;
            default:
                return false;
        }
    }

    private void moveFile(FileResource file, UserAccount user) {
        java.io.File source = new java.io.File(file.getPath());
        if (!source.exists() || !source.isFile()) {
            System.out.println(Colors.red(">> [ERROR] Source file not found: " + file.getPath()));
            return;
        }
        java.io.File destDir = new java.io.File("workspace/");
        if (!destDir.exists()) destDir.mkdirs();
        java.io.File dest = new java.io.File("workspace/" + source.getName());
        try {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println(Colors.green(">> [MOVE SUCCESS] File saved to: " + dest.getAbsolutePath()));
        } catch (java.io.IOException e) {
            System.out.println(Colors.red(">> [MOVE ERROR] " + e.getMessage()));
        }
    }

    // Legacy support
    @Override
    public void openFile(FileResource file, User user) {
        System.out.println(Colors.yellow(">> [INFO] Use execute(Operation, FileResource, UserAccount) instead."));
    }
}