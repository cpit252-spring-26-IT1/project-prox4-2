package sa.edu.kau.fcit.cpit252.project;
import java.awt.Desktop;
import java.io.File;

public class RealFileAccess implements FileAccess {
    @Override
    public void openFile(FileResource file, User user) {
        try {
            File f = new File(file.getPath());
            if (f.exists()) {
                System.out.println(">> [FILE FOUND] Path: " + f.getAbsolutePath());
                System.out.println(">> [OPENING] Please wait while the system opens the file...");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(f);
                } else {
                    System.out.println(">> [NOTICE] Automated file opening is not supported on this OS.");
                }
            } else {
                System.out.println(">> [ERROR] The file does not exist at the specified path: " + file.getPath());
            }
        } catch (Exception e) {
            System.out.println(">> [SYSTEM ERROR] Could not open file: " + e.getMessage());
        }
    }
}