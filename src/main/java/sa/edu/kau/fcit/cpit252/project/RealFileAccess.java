package sa.edu.kau.fcit.cpit252.project;

public class RealFileAccess implements FileAccess {
    @Override
    public void openFile(FileResource file, User user) {
        System.out.println(">> Opening file done successfully: " + file.getName() + " File opened for: " + user.getUsername());
    }
}
