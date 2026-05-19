package sa.edu.kau.fcit.cpit252.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DownloadProxy {

    private static final String DOWNLOAD_DIR = "downloads/";

    public void downloadFile(FileResource file, User user) {
        System.out.println("++ [DOWNLOAD PROXY] Checking download permissions for: " + file.getName());

        if (!canDownload(user, file)) {
            System.out.println("XX [DOWNLOAD DENIED] Role [" + user.getRole() + "] is not authorized to download " + file.getType() + " files.");
            return;
        }

        try {
            File source = new File(file.getPath());

            if (!source.exists() || !source.isFile()) {
                System.out.println(">> [ERROR] Source file not found: " + file.getPath());
                return;
            }

            File downloadFolder = new File(DOWNLOAD_DIR);
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs();
            }

            File destination = new File(DOWNLOAD_DIR + source.getName());
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("++ [DOWNLOAD SUCCESS] File saved to: " + destination.getAbsolutePath());

        } catch (IOException e) {
            System.out.println(">> [DOWNLOAD ERROR] Could not download file: " + e.getMessage());
        }
    }

    private boolean canDownload(User user, FileResource file) {
        switch (file.getType()) {
            case SENSITIVE:
                return user.getRole() == Role.MANAGER;
            case INTERNAL:
                return user.getRole() == Role.MANAGER || user.getRole() == Role.USER;
            case NORMAL:
                return user.getRole() != Role.GUEST;
            default:
                return false;
        }
    }
}