package sa.edu.kau.fcit.cpit252.project;

public class FileLockManager {

    public static boolean lockFile(FileResource file, UserAccount requester) {
        if (requester.getRole() != Role.MANAGER && requester.getRole() != Role.OWNER) {
            System.out.println(Colors.red("XX [DENIED] Only MANAGER or OWNER can lock files."));
            return false;
        }
        if (file.isLocked()) {
            System.out.println(Colors.yellow(">> [INFO] File '" + file.getName() + "' is already locked."));
            return false;
        }
        file.setLocked(true);
        System.out.println(Colors.yellow(">> [LOCKED] File '" + file.getName() + "' is now locked."));
        return true;
    }

    public static boolean unlockFile(FileResource file, UserAccount requester) {
        if (requester.getRole() != Role.MANAGER && requester.getRole() != Role.OWNER) {
            System.out.println(Colors.red("XX [DENIED] Only MANAGER or OWNER can unlock files."));
            return false;
        }
        if (!file.isLocked()) {
            System.out.println(Colors.yellow(">> [INFO] File '" + file.getName() + "' is not locked."));
            return false;
        }
        file.setLocked(false);
        System.out.println(Colors.green(">> [UNLOCKED] File '" + file.getName() + "' is now unlocked."));
        return true;
    }
}