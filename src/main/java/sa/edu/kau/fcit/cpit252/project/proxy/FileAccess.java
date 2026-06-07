package sa.edu.kau.fcit.cpit252.project.proxy;

import sa.edu.kau.fcit.cpit252.project.files.FileResource;
import sa.edu.kau.fcit.cpit252.project.auth.UserAccount;

public interface FileAccess {
    void openFile(FileResource file, UserAccount user);
}
