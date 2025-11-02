package ca.corbett.packager.io;

import ca.corbett.extras.progress.SimpleProgressWorker;
import ca.corbett.packager.project.Project;
import ca.corbett.updates.UpdateSources;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FtpUploadThread extends SimpleProgressWorker {

    private static final Logger log = Logger.getLogger(FtpUploadThread.class.getName());

    private final Project project;
    private final UpdateSources.UpdateSource updateSource;
    private final FtpParams ftpParams;

    public FtpUploadThread(Project project, UpdateSources.UpdateSource updateSource, FtpParams params) {
        this.project = project;
        this.updateSource = updateSource;
        this.ftpParams = params;
    }

    @Override
    public void run() {
        fireProgressBegins(4);
        FtpUtil ftp = null;

        try {
            // Sanity checks off the bat:
            if (!project.getDistDir().exists() || !project.getDistDir().isDirectory()) {
                throw new IOException("Project dist directory does not exist or is not readable.");
            }
            if (!project.getVersionManifestFile().exists() || !project.getVersionManifestFile().isFile()) {
                throw new IOException("Version manifest does not exist or can't be read.");
            }
            if (!project.getExtensionsDir().exists() || !project.getExtensionsDir().isDirectory()) {
                throw new IOException("Project extensions dir does not exist or can't be read.");
            }
            if (ftpParams == null || ftpParams.host == null || ftpParams.host.isBlank()) {
                throw new IOException("No ftp host given; unable to connect.");
            }

            // Set the generated timestamp in the version manifest:
            project.getVersionManifest().setManifestGenerated(Instant.now());
            project.saveVersionManifest();

            // Connect and log in:
            ftp = new FtpUtil();
            ftp.connect(ftpParams);

            // Clean the existing target directory:
            ftp.cleanDirectory(ftpParams.targetDir);

            // Public key (optional):
            if (updateSource.getPublicKeyRelativePath() != null) {
                if (project.getPublicKeyFile().exists()) {
                    ftp.uploadFile(project.getPublicKeyFile(), ftpParams.targetDir);
                }
            }
            fireProgressUpdate(1, "Uploading project files");

            // Version manifest (mandatory):
            ftp.uploadFile(project.getVersionManifestFile(), ftpParams.targetDir);
            fireProgressUpdate(2, "Uploading project files");

            // Now upload the entire extensions dir recursively:
            ftp.uploadDirectory(project.getExtensionsDir(), ftpParams.targetDir);

            fireProgressComplete();
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "FTP upload failed: " + ioe.getMessage(), ioe);
            fireProgressError("FTP Upload", "Upload failed: " + ioe.getMessage());
            fireProgressComplete();
        }
        finally {
            if (ftp != null) {
                try {
                    ftp.disconnect();
                }
                catch (IOException ioe) {
                    log.log(Level.SEVERE, "Unable to disconnect from FTP host!", ioe);
                }
            }
        }
    }
}
