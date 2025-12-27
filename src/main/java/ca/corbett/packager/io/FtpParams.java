package ca.corbett.packager.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.properties.FileBasedProperties;
import ca.corbett.packager.project.Project;
import ca.corbett.updates.UpdateSources;

import java.io.File;
import java.io.IOException;

/**
 * A utility class for encapsulating and handling FTP parameters, such as hostname, username,
 * password, and target directory. Utility methods are provided to load, save, and check
 * for the existence of FTP parameters associated with a specific Project and UpdateSource.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class FtpParams {
    public static final String PROP_HOST = "Ftp.Host";
    public static final String PROP_USERNAME = "Ftp.Username";
    public static final String PROP_PASSWORD = "Ftp.Password";
    public static final String PROP_TARGET_DIR = "Ftp.TargetDirectory";

    public String host;
    public String username;
    public String password;
    public String targetDir;

    /**
     * Attempts to load the saved FtpParams for the given Project and UpdateSource.
     * If none are found, a new (empty) FtpParams object is created on disk and returned.
     */
    public static FtpParams fromUpdateSource(Project project, UpdateSources.UpdateSource source) throws IOException {
        FileBasedProperties props = getPropsInstance(project, source);
        FtpParams params = new FtpParams();
        params.host = props.getString(PROP_HOST, "");
        params.username = props.getString(PROP_USERNAME, "");
        params.password = props.getString(PROP_PASSWORD, "");
        params.targetDir = props.getString(PROP_TARGET_DIR, "");
        return params;
    }

    /**
     * Saves the given FtpParams to the given Project and UpdateSource.
     * This will update the props file if it exists, or create a new one if it doesn't.
     */
    public static void save(Project project, UpdateSources.UpdateSource source, FtpParams params) throws IOException {
        FileBasedProperties props = getPropsInstance(project, source);
        props.setString(PROP_HOST, params.host);
        props.setString(PROP_USERNAME, params.username);
        props.setString(PROP_PASSWORD, params.password);
        props.setString(PROP_TARGET_DIR, params.targetDir);
        props.saveWithoutException();
    }

    /**
     * Reports whether saved FtpParams exist for the given Project and UpdateSource.
     */
    public static boolean ftpParamsExist(Project project, UpdateSources.UpdateSource source) {
        return getPropsFile(project, source).exists();
    }

    /**
     * Gets the FileBasedProperties instance for the given Project and UpdateSource, or creates
     * one if it doesn't exist yet.
     */
    protected static FileBasedProperties getPropsInstance(Project project, UpdateSources.UpdateSource source)
            throws IOException {
        FileBasedProperties props = new FileBasedProperties(getPropsFile(project, source));
        if (!ftpParamsExist(project, source)) {
            props.setString(PROP_HOST, "");
            props.setString(PROP_USERNAME, "");
            props.setString(PROP_PASSWORD, "");
            props.setString(PROP_TARGET_DIR, "");
            props.saveWithoutException();
        }
        else {
            props.load();
        }
        return props;
    }

    /**
     * Invoked internally to get the props file for the given Project and UpdateSource.
     * No check is done here to see if the file actually exists or not - rather, we return
     * the file location where it *should* be.
     * <p>
     * Invalid characters in the data source name are sanitized to ensure a valid filename.
     * So, the resulting props file name may not perfectly match the source name,
     * but it should be pretty close.
     * </p>
     */
    private static File getPropsFile(Project project, UpdateSources.UpdateSource source) {
        return new File(project.getProjectDir(), FileSystemUtil.sanitizeFilename(source.getName()) + ".props");
    }
}
