package sa.edu.kau.fcit.cpit252.project;

public class AlertObserver implements AccessObserver {

    @Override
    public void onAccessEvent(AccessEvent event, User user, FileResource file) {
        switch (event) {
            case ACCESS_DENIED:
                System.out.println("!! [SECURITY ALERT] Unauthorized access attempt by '"
                        + user.getUsername() + "' (Role: " + user.getRole()
                        + ") on file: " + file.getName());
                break;
            case LIMIT_REACHED:
                System.out.println("!! [SECURITY ALERT] View limit exceeded by '"
                        + user.getUsername() + "' for file: " + file.getName()
                        + ". Further access is blocked.");
                break;
            case ACCESS_GRANTED:
                break;
        }
    }
}