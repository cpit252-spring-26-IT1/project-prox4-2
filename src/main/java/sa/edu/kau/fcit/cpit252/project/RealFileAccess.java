package sa.edu.kau.fcit.cpit252.project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
public class RealFileAccess implements FileAccess {
    @Override
    public void openFile(FileResource file, User user) {
        try {
            File f = new File(file.getPath());

            if (!f.exists()) {
                System.out.println(">> [ERROR] The file does not exist at the specified path: " + file.getPath());
                return;
            }
            if (!f.isFile()) {
                System.out.println(">> [ERROR] The specified path is not a file: " + file.getPath());
                return;
            }
            if (!f.canRead()) {
                System.out.println(">> [ERROR] No read permission for file: " + file.getPath());
                return;
            }    
              System.out.println(">> [FILE FOUND] Path: " + f.getAbsolutePath());
            System.out.println(">> [OPENING] Reading file content...");
            System.out.println("------------------- FILE CONTENT -------------------");
            
            List<String> lines = Files.readAllLines(f.toPath());
            if (lines.isEmpty()) {
                System.out.println("(File is empty)");
            } else {
                for (String line : lines) {
                    System.out.println(line);
                }
            }
            
            System.out.println("------------------- END OF FILE --------------------");
            
        } catch (IOException e) {
            System.out.println(">> [SYSTEM ERROR] IO problem while reading file: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println(">> [SYSTEM ERROR] Security restriction: " + e.getMessage());
        }
    }
}