package sa.edu.kau.fcit.cpit252.project.observer;

import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;
import sa.edu.kau.fcit.cpit252.project.files.FileResource;
import sa.edu.kau.fcit.cpit252.project.ui.Colors;

public class AlertObserver implements AccessObserver {

    @Override
    public void onAccessEvent(AccessEvent event, UserAccount user, FileResource file) {
        String username = user != null ? user.getUsername() : "SYSTEM";
        String fileName = file != null ? file.getName() : "N/A";

        switch (event) {
            case ACCESS_DENIED:
                System.out.println(Colors.red("!! [SECURITY ALERT] Unauthorized access attempt by '"
                        + username + "' on file: " + fileName));
                break;
            case LIMIT_REACHED:
                System.out.println(Colors.red("!! [SECURITY ALERT] View limit exceeded by '"
                        + username + "' for file: " + fileName));
                break;
            case ACCOUNT_LOCKED:
                System.out.println(Colors.red("!! [SECURITY ALERT] Account '" + username + "' has been locked."));
                break;
            case USER_CREATED:
                System.out.println(Colors.green(">> [AUDIT] New user created: " + username));
                break;
            case USER_PROMOTED:
                System.out.println(Colors.green(">> [AUDIT] User promoted: " + username));
                break;
            case USER_DEMOTED:
                System.out.println(Colors.yellow(">> [AUDIT] User demoted: " + username));
                break;
            case FILE_LOCKED:
                System.out.println(Colors.yellow(">> [AUDIT] File locked: " + fileName));
                break;
            case FILE_UNLOCKED:
                System.out.println(Colors.green(">> [AUDIT] File unlocked: " + fileName));
                break;
            default:
                break;
        }
    }
}