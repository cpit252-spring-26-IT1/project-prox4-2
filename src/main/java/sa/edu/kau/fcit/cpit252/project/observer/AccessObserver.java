package sa.edu.kau.fcit.cpit252.project.observer;

import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;
import sa.edu.kau.fcit.cpit252.project.files.FileResource;
public interface AccessObserver {
    void onAccessEvent(AccessEvent event, User user, FileResource file);
}
