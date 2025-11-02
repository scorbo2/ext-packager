package ca.corbett.packager.io;

import ca.corbett.updates.UpdateSources;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class FtpUtil {

    private static final Logger log = Logger.getLogger(FtpUtil.class.getName());

    private final FTPClient ftpClient;
    private FtpParams ftpParams;

    public FtpUtil() {
        ftpClient = new FTPClient();
    }

    public void connect(FtpParams params) throws IOException {
        disconnect();

        this.ftpParams = params;
        log.info("Attempting connection to \"" + ftpParams.host + "\" as user \"" + ftpParams.username + "\"...");
        ftpClient.connect(ftpParams.host);
        ftpClient.login(ftpParams.username, ftpParams.password);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    public boolean isConnected() {
        return ftpClient.isConnected();
    }

    public void disconnect() throws IOException {
        if (isConnected()) {
            log.info("Disconnecting from host \"" + ftpParams.host + "\"");
            ftpClient.logout();
            ftpClient.disconnect();
        }
        ftpParams = null;
    }

    /**
     * Uploads the contents of the given dist directory to the remote host.
     * This will remove all files in the target directory specified in our ftp params!
     * The workflow is: clean the target directory, then batch upload all files.
     * In future (see <a href="https://github.com/scorbo2/ext-packager/issues/8">Issue 8</a>,
     * this will be smartened up to only upload new/modified files. For now it's nuke and pave.
     */
    public void upload(UpdateSources.UpdateSource updateSource, File distDir) throws IOException {
        if (!isConnected() || ftpParams == null) {
            throw new IOException("Not connected.");
        }

        if (!distDir.exists() || !distDir.isDirectory()) {
            throw new IOException("The given dist directory does not exist or can't be read.");
        }
        File publicKeyFile = null;
        if (updateSource.getPublicKeyRelativePath() != null) {
            publicKeyFile = new File(distDir, updateSource.getPublicKeyRelativePath());
            if (!publicKeyFile.exists()) {
                publicKeyFile = null;
            }
        }
        File manifestFile = new File(distDir, updateSource.getVersionManifestRelativePath());
        if (!manifestFile.exists() || !manifestFile.isFile() || !manifestFile.canRead()) {
            throw new IOException("Manifest file does not exist or can't be read.");
        }
        File extensionsDir = new File(distDir, "extensions");
        if (!extensionsDir.exists() || !extensionsDir.isDirectory()) {
            throw new IOException("The extension directory does not exist or can't be read.");
        }

        // Clean the existing target directory:
        cleanDirectory(ftpParams.targetDir);

        // Upload public key (if present) and manifest file (must be present):
        if (publicKeyFile != null) {
            uploadFile(publicKeyFile, ftpParams.targetDir);
        }
        uploadFile(manifestFile, ftpParams.targetDir);

        // Now upload the entire extensions dir recursively:
        uploadDirectory(extensionsDir, ftpParams.targetDir);
    }

    /**
     * Uploads the given local directory recursively.
     */
    public void uploadDirectory(File localDir, String remoteParentDir) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }
        if (!localDir.exists() || !localDir.isDirectory()) {
            throw new IOException("Given local directory does not exist or is not readable.");
        }
        log.info("Begin FTP upload of directory: " + localDir.getAbsolutePath());

        // Make the target dir if it isn't there already:
        String targetRemoteDir = remoteParentDir;
        if (!targetRemoteDir.endsWith("/")) {
            targetRemoteDir += "/";
        }
        targetRemoteDir += localDir.getName();
        ftpClient.makeDirectory(targetRemoteDir);

        // Now upload all contents:
        File[] files = localDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    uploadFile(file, targetRemoteDir);
                }
                else if (file.isDirectory()) {
                    uploadDirectory(file, targetRemoteDir); // recurse!
                }
            }
        }
    }

    /**
     * Uploads the given local file to the given remote directory. The filename will be preserved.
     */
    public void uploadFile(File localFile, String remoteParentDir) throws IOException {
        if (!remoteParentDir.endsWith("/")) {
            remoteParentDir += "/";
        }
        log.info("FTP upload: " + localFile.getName() + " -> " + remoteParentDir);
        try (InputStream is = new BufferedInputStream(new FileInputStream(localFile))) {
            ftpClient.storeFile(remoteParentDir + localFile.getName(), is);
        }
    }

    /**
     * Reports whether the given named directory exists as a direct child of the given parent directory.
     * There doesn't seem to be an easy way to get this from FTPClient, so we enumerate all child
     * directories and look for the child in question.
     */
    private boolean childDirExists(String parentDir, String childDir) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }

        FTPFile[] childDirs = ftpClient.listDirectories(parentDir);
        for (FTPFile candidateDir : childDirs) {
            if (candidateDir.isDirectory() && candidateDir.getName().equals(childDir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * "Cleans" the given target directory by deleting all of its contents recursively, without removing
     * the target directory itself.
     */
    public void cleanDirectory(String targetDir) throws IOException {
        cleanDirectory(targetDir, false);
    }

    /**
     * "Cleans" the given target directory by deleting all of its contents recursively, and then
     * also optionally removes the given target directory as well.
     */
    public void cleanDirectory(String targetDir, boolean alsoRemoveTargetDir) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }

        deleteDirectory(targetDir, alsoRemoveTargetDir);
    }

    /**
     * Invoked internally to recursively delete the contents of the given directory, and also
     * optionally remove the directory itself.
     */
    private void deleteDirectory(String targetDir, boolean alsoRemoveTargetDir) throws IOException {
        log.info("Deleting remote directory: " + targetDir);
        FTPFile[] files = ftpClient.listFiles(targetDir);

        if (files != null) {
            for (FTPFile file : files) {
                String currentFileName = file.getName();

                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    continue;
                }

                String filePath = targetDir + "/" + currentFileName;

                if (file.isDirectory()) {
                    deleteDirectory(filePath, true);
                } else {
                    if (!ftpClient.deleteFile(filePath)) {
                        throw new IOException("Failed to delete file: " + filePath);
                    }
                }
            }
        }

        if (alsoRemoveTargetDir) {
            if (!ftpClient.removeDirectory(targetDir)) {
                throw new IOException("Failed to delete directory: " + targetDir);
            }
        }
    }
}
