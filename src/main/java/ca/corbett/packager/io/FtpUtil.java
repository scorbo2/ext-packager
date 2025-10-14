package ca.corbett.packager.io;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class FtpUtil {

    private final FTPClient ftpClient;

    public FtpUtil() {
        ftpClient = new FTPClient();
    }

    /**
     * Just testing, ignore me for now
     */
    public void testUpload(List<File> generatedFiles) throws SocketException, IOException {
        try {
            ftpClient.connect("ftp.example.com", 21);
            ftpClient.login("username", "password");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Create temp directory
            String tempDir = "/path/to/target_temp_" + System.currentTimeMillis();
            ftpClient.makeDirectory(tempDir);

            // Upload all files to temp directory
            for (File localFile : generatedFiles) {
                FileInputStream inputStream = new FileInputStream(localFile);
                ftpClient.storeFile(tempDir + "/" + localFile.getName(), inputStream);
                inputStream.close();
            }

            // Atomic swap: delete old directory, rename temp to final name
            deleteDirectory(ftpClient, "/path/to/target"); // recursive delete
            ftpClient.rename(tempDir, "/path/to/target");

            ftpClient.logout();
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        }
    }

    private void deleteDirectory(FTPClient ftpClient, String parentDir) throws IOException {
        FTPFile[] files = ftpClient.listFiles(parentDir);

        if (files != null) {
            for (FTPFile file : files) {
                String currentFileName = file.getName();

                // Skip "." and ".." entries
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    continue;
                }

                String filePath = parentDir + "/" + currentFileName;

                if (file.isDirectory()) {
                    // Recursively delete subdirectory contents
                    deleteDirectory(ftpClient, filePath);
                    // Then remove the empty directory
                    ftpClient.removeDirectory(filePath);
                } else {
                    // Delete file
                    ftpClient.deleteFile(filePath);
                }
            }
        }

        // Finally, delete the parent directory itself
        ftpClient.removeDirectory(parentDir);
    }
}
