package ca.corbett.packager.io;

import ca.corbett.extras.progress.SimpleProgressWorker;
import ca.corbett.packager.project.Project;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A worker thread for local filesystem deployment. It is assumed that the supplied
 * UpdateSource will have a baseUrl that is a filesystem url! A progress error will
 * be thrown immediately otherwise.
 * <p>
 * The project public key (if it exists), version manifest, and all extension jars
 * and support files will be copied to the given baseUrl.
 * </p>
 * <p>
 * <b>NOTE:</b> The given baseUrl directory will be cleaned before copying begins.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class FileSystemUploadThread extends SimpleProgressWorker {

    private static final Logger log = Logger.getLogger(FileSystemUploadThread.class.getName());

    private final Project project;
    private final File targetDir;

    public FileSystemUploadThread(Project project, File targetDir) {
        this.project = project;
        this.targetDir = targetDir;
    }

    @Override
    public void run() {
        fireProgressBegins(4);

        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            fireProgressError("Filesystem upload",
                              "Given target directory does not exist or can't be read.");
            fireProgressComplete();
            return;
        }

        try {
            // Set the generated timestamp in the version manifest:
            project.getVersionManifest().setManifestGenerated(Instant.now());
            project.saveVersionManifest();

            log.info("Cleaning target directory: " + targetDir.getAbsolutePath());
            FileUtils.cleanDirectory(targetDir);
            fireProgressUpdate(1, "Copying distribution files");

            if (project.getPublicKeyFile().exists()) {
                log.info("Copying public key...");
                FileUtils.copyFile(project.getPublicKeyFile(),
                                   new File(targetDir, project.getPublicKeyFile().getName()));
            }
            fireProgressUpdate(2, "Copying distribution files");

            log.info("Copying version manifest...");
            FileUtils.copyFile(project.getVersionManifestFile(),
                               new File(targetDir, project.getVersionManifestFile().getName()));
            fireProgressUpdate(3, "Copying distribution files");

            log.info("Copying extensions directory...");
            FileUtils.copyDirectory(project.getExtensionsDir(), new File(targetDir, "extensions"));

            fireProgressComplete();
        }
        catch (IOException ioe) {
            String err = "File system copy error: " + ioe.getMessage();
            log.log(Level.SEVERE, err, ioe);
            fireProgressError("File system upload", err);
            fireProgressComplete();
        }
    }
}
