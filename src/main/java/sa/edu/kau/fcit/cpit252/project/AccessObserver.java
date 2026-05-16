package sa.edu.kau.fcit.cpit252.project;

public interface AccessObserver {
    void onAccessEvent(AccessEvent event, User user, FileResource file);
}
