package ca.corbett.packager.project;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.CoalescingDocumentListener;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.packager.ui.MainWindow;
import ca.corbett.updates.VersionManifest;

import javax.swing.Timer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ca.corbett.updates.VersionManifest.ApplicationVersion;
import static ca.corbett.updates.VersionManifest.Extension;
import static ca.corbett.updates.VersionManifest.ExtensionVersion;

/**
 * A singleton class that manages access to one singular currently loaded Project.
 * Callers can subscribe to receive notifications from this class when a Project
 * is created or loaded.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ProjectManager {

    private static final Logger log = Logger.getLogger(ProjectManager.class.getName());
    private static ProjectManager instance;

    private final List<ProjectListener> projectListeners = new ArrayList<>();
    private Project project;
    private boolean isSaveInProgress = false;
    private boolean isLoadInProgress = false;

    private ProjectManager() {

    }

    public static ProjectManager getInstance() {
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }

    /**
     * Returns the current Project, or null if one is not open.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Creates a new, empty project with the given name in the given location.
     */
    public void newProject(String name, File projectDir) throws IOException {
        project = Project.createNew(name, projectDir);
        MainWindow.getInstance().projectOpened();
        fireProjectLoadedEvent(project);
    }

    /**
     * Attempts to load the given ext-packager Project from the given project file.
     */
    public void loadProject(File projectFile) throws IOException {
        if (isLoadInProgress) {
            return;
        }
        log.info("Loading project: " + projectFile.getAbsolutePath());
        isLoadInProgress = true;
        try {
            project = Project.fromFile(projectFile);
            MainWindow.getInstance().projectOpened();
            fireProjectLoadedEvent(project);
        }
        finally {
            // Extremely cheesy, but we have to wait for coalescing doc listeners to finish responding to the save.
            Timer timer = new Timer(CoalescingDocumentListener.DELAY_MS * 2, e -> isLoadInProgress = false);
            timer.setRepeats(false);
            timer.start();
        }
    }

    /**
     * Saves any changes to the current Project, if one is open.
     */
    public void save() throws IOException {
        if (project == null || isSaveInProgress || isLoadInProgress) {
            return;
        }
        log.info("Saving current project");
        isSaveInProgress = true;
        try {
            project.save();
            fireProjectSavedEvent(project);
        }
        finally {
            // Extremely cheesy, but we have to wait for coalescing doc listeners to finish responding to the save.
            Timer timer = new Timer(CoalescingDocumentListener.DELAY_MS * 2, e -> isSaveInProgress = false);
            timer.setRepeats(false);
            timer.start();
        }
    }

    /**
     * Shorthand to return the string contents of the current public key.
     * Will return empty string if no Project is loaded or if the current
     * Project does not define a public key.
     */
    public String getPublicKeyAsString() {
        if (project == null
                || project.getPublicKey() == null
                || !project.getPublicKeyFile().exists()) {
            return "";
        }
        try {
            return FileSystemUtil.readFileToString(project.getPublicKeyFile());
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to read public key file: " + ioe.getMessage(), ioe);
            return "";
        }
    }

    /**
     * Shorthand to return the string contents of the current private key.
     * Will return empty string if no Project is loaded or if the current
     * Project does not define a private key.
     */
    public String getPrivateKeyAsString() {
        if (project == null
                || project.getPrivateKey() == null
                || !project.getPrivateKeyFile().exists()) {
            return "";
        }
        try {
            return FileSystemUtil.readFileToString(project.getPrivateKeyFile());
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to read private key file: " + ioe.getMessage(), ioe);
            return "";
        }
    }

    /**
     * Shorthand to return the string contents of the current UpdateSources.
     */
    public String getUpdateSourcesAsString() {
        if (project == null || !project.getUpdateSourcesFile().exists()) {
            return "";
        }
        try {
            return FileSystemUtil.readFileToString(project.getUpdateSourcesFile());
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to read update sources file: " + ioe.getMessage(), ioe);
            return "";
        }
    }

    /**
     * Shorthand to return the string contents of the current VersionManifest.
     */
    public String getVersionManifestAsString() {
        if (project == null || !project.getVersionManifestFile().exists()) {
            return "";
        }
        try {
            return FileSystemUtil.readFileToString(project.getVersionManifestFile());
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to read version manifest file: " + ioe.getMessage(), ioe);
            return "";
        }
    }

    public AppExtensionInfo getExtInfoFromJar(File jarFile) throws Exception {
        return getExtInfoFromJar(jarFile, null);
    }

    /**
     * Convenience method to extract the extInfo json file from the given jar file, if possible.
     * Exception will be thrown if the jar can't be read or if the json is missing or can't be parsed.
     * If expectedAppName is not null, then the targetAppVersion of the given jar will be tested
     * against expectedAppName. If they don't match, an exception is thrown.
     */
    public AppExtensionInfo getExtInfoFromJar(File jarFile, String expectedAppName) throws Exception {
        if (jarFile == null || !jarFile.exists() || !jarFile.canRead() || !jarFile.isFile()) {
            throw new Exception("getExtInfoFromJar: supplied jar does not exist or can't be read.");
        }

        // Parse extInfo.json out of this jar:
        String extInfoStr = FileSystemUtil.extractTextFileFromJar("extInfo.json", jarFile);
        if (extInfoStr == null) {
            throw new Exception("Jar file "
                                        + jarFile.getAbsolutePath()
                                        + " does not contain extInfo.json - this is not a valid extension jar.");
        }

        // Make sure it targets the expected application:
        // But only if we were given one to expect:
        AppExtensionInfo extInfo = AppExtensionInfo.fromJson(extInfoStr);
        if (expectedAppName != null) {
            if (!expectedAppName.equals(extInfo.getTargetAppName())) {
                throw new Exception("Unable to import Jar file "
                                            + jarFile.getAbsolutePath()
                                            + " because it targets the wrong application: "
                                            + extInfo.getTargetAppName());
            }
        }

        return extInfo;
    }

    /**
     * Attempts to import the given extensionJar into the given versionManifest.
     * If any error occurs, an Exception is thrown and the given versionManifest is not modified.
     */
    public ExtensionVersion importExtensionJar(VersionManifest versionManifest, File extensionJar) throws Exception {
        AppExtensionInfo extInfo = getExtInfoFromJar(extensionJar, versionManifest.getApplicationName());
        ApplicationVersion appVersion = findOrCreateApplicationVersion(versionManifest, extInfo.getTargetAppVersion());
        String appVer = appVersion.getVersion();
        Extension extension = findOrCreateExtension(appVersion, extInfo.getName());
        ExtensionVersion extensionVersion = findOrCreateExtensionVersion(extension, extInfo, extensionJar, appVer);
        copyJarToProjectDirectory(extensionVersion, extensionJar, appVer);
        return extensionVersion;
    }

    /**
     * Searches the given VersionManifest for an ApplicationVersion with the specified target version,
     * and returns it if it exists. If it does not exist, it will be created, added to the manifest, and returned.
     */
    public ApplicationVersion findOrCreateApplicationVersion(VersionManifest manifest, String targetAppVersion) {
        for (ApplicationVersion version : manifest.getApplicationVersions()) {
            if (version.getVersion().equals(targetAppVersion)) {
                return version;
            }
        }

        // Does not exist yet; create it:
        log.info("Creating application version: " + targetAppVersion);
        ApplicationVersion version = new ApplicationVersion();
        version.setVersion(targetAppVersion);
        manifest.addApplicationVersion(version);
        return version;
    }

    /**
     * Searches the given ApplicationVersion for an Extension with the given name, and returns
     * it if it exists. If it does not exist, it will be created, added to the ApplicationVersion, and returned.
     */
    public Extension findOrCreateExtension(ApplicationVersion appVersion, String extensionName) {
        for (Extension ext : appVersion.getExtensions()) {
            if (ext.getName().equals(extensionName)) {
                return ext;
            }
        }

        // Extension doesn't exist, create it
        log.info("Creating extension: " + extensionName + " for application version " + appVersion.getVersion());
        VersionManifest.Extension extension = new VersionManifest.Extension();
        extension.setName(extensionName);
        appVersion.addExtension(extension);
        return extension;
    }

    /**
     * Searches the given Extension for an ExtensionVersion that matches the supplied extInfo,
     * and returns it if it exists. If it does not exist, it will be created, added to the Extension, and returned.
     */
    public ExtensionVersion findOrCreateExtensionVersion(Extension extension, AppExtensionInfo extInfo, File jarFile, String appVersion) {
        for (ExtensionVersion version : extension.getVersions()) {
            if (version.getExtInfo().getName().equals(extInfo.getName())
                    && version.getExtInfo().getVersion().equals(extInfo.getVersion())) {
                log.warning("Found existing extension " + extInfo.getName() + " version " + extInfo.getVersion());
                return version;
            }
        }

        // Get base path:
        String basePath = ProjectManager.getInstance().getProject().getExtensionsDir()
                                        .getName() + "/" + appVersion + "/";

        // Extension version doesn't exist, create it
        log.info("Creating new extension version: " + extInfo.getName() + " version " + extInfo.getVersion());
        VersionManifest.ExtensionVersion extVersion = new VersionManifest.ExtensionVersion();
        extVersion.setExtInfo(extInfo);
        extVersion.setDownloadPath(basePath + jarFile.getName());

        // See if there's a signature file here:
        File signatureFile = new File(jarFile.getParentFile(), getBasename(jarFile.getName()) + ".sig");
        if (signatureFile.exists()) {
            extVersion.setSignaturePath(basePath + signatureFile.getName());
        }

        extension.addVersion(extVersion);
        return extVersion;
    }

    /**
     * Cleans up all files associated with the given ApplicationVersion within our ExtPackager project directory.
     */
    public void removeApplicationVersion(ApplicationVersion appVersion) throws IOException {
        for (Extension extension : appVersion.getExtensions()) {
            removeExtension(extension);
        }
        File extDir = new File(getProject().getExtensionsDir(), appVersion.getVersion());
        if (extDir.exists() && extDir.isDirectory()) {
            Files.delete(extDir.toPath());
        }
    }

    /**
     * Cleans up all files associated with the given Extension within our ExtPackager project directory.
     */
    public void removeExtension(Extension extension) throws IOException {
        for (ExtensionVersion extensionVersion : extension.getVersions()) {
            removeExtensionVersion(extensionVersion);
        }
    }

    /**
     * Cleans up all files associated with the given ExtensionVersion within our ExtPackager project directory.
     */
    public void removeExtensionVersion(ExtensionVersion extensionVersion) throws IOException {
        File parentDir = new File(getProject().getExtensionsDir(), extensionVersion.getExtInfo().getTargetAppVersion());
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            return;
        }
        if (extensionVersion.getDownloadPath() == null) {
            return; // wonky case but this may not be set if the json is invalid
        }
        String jarPath = extensionVersion.getDownloadPath();
        Files.delete(new File(parentDir, jarPath).toPath());
        File signatureFile = new File(parentDir, getBasename(jarPath) + ".sig");
        if (signatureFile.exists()) {
            Files.delete(signatureFile.toPath());
        }
        removeAllScreenshots(extensionVersion);
    }

    /**
     * Finds and deletes any screenshot images associated with the given ExtensionVersion.
     */
    public void removeAllScreenshots(ExtensionVersion extensionVersion) throws IOException {
        File parentDir = new File(getProject().getExtensionsDir(), extensionVersion.getExtInfo().getTargetAppVersion());
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            return;
        }
        if (extensionVersion.getDownloadPath() == null) {
            return; // wonky case but this may not be set if the json is invalid
        }
        String basename = getBasename(extensionVersion.getDownloadPath());
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File toDelete : files) {
                String fileName = toDelete.getName();
                // Check if the file name matches the base name followed by an underscore
                if (fileName.startsWith(basename + "_")) {
                    Files.delete(toDelete.toPath());
                }
            }
        }
    }

    /**
     * Copies the given jar to our project's extensions directory, into a subdirectory named
     * after the given application version. If any screenshots exist for the given jar (matching
     * the jar's basename but with an image extension), they will also be copied.
     */
    public void copyJarToProjectDirectory(ExtensionVersion extensionVersion, File jar, String appVersion)
            throws IOException {
        File extensionsDir = ProjectManager.getInstance().getProject().getExtensionsDir();
        File appVersionDir = new File(extensionsDir, appVersion);
        if (!appVersionDir.exists()) {
            appVersionDir.mkdirs();
        }
        // Get base path:
        String basePath = ProjectManager.getInstance().getProject().getExtensionsDir()
                                        .getName() + "/" + appVersion + "/";

        // Copy the jar itself:
        File targetFile = new File(appVersionDir, jar.getName());
        Files.copy(jar.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Also look for screenshots to import:
        for (File screenshot : findScreenshots(jar)) {
            log.info("Importing extension screenshot: " + screenshot.getName());
            targetFile = new File(appVersionDir, screenshot.getName());
            Files.copy(screenshot.toPath(),
                       targetFile.toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
            extensionVersion.addScreenshot(basePath + targetFile.getName());
        }
    }

    /**
     * Returns a list of screenshots associated with the given jar file, if any.
     * Any image file of a supported type (jpg, png, or gif) that is found with the same base
     * name as the given jar file will be considered a match. <b>An underscore must stand between
     * the base name and the rest of the filename!</b> For example, given a
     * jar file named "myExtension.jar":
     * <ul>
     *     <li><b>myExtension.jpg</b> would NOT match - no underscore
     *     <li><b>myExtension_1.jpg</b> would match
     *     <li><b>myExtension_superAwesomeScreenshot.jpg</b> would match
     *     <li><b>MyExtension_1.jpg</b> would NOT match - wrong case (My instead of my)
     *     <li><b>myExt1.jpg</b> would NOT match - incomplete base name
     *     <li><b>myExtension_1.tiff</b> would NOT match - unsupported image type
     * </ul>
     * <p>
     * If no matching screenshots are found, an empty list is returned.
     * </p>
     */
    public List<File> findScreenshots(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.canRead() || jarFile.isDirectory()) {
            return List.of();
        }
        File[] files = jarFile.getParentFile().listFiles();
        if (files == null) {
            return List.of();
        }

        final String basename = getBasename(jarFile.getName());
        return Arrays.stream(files)
                     .filter(f -> f.getName().startsWith(basename) && isImageFile(f))
                     .toList();
    }

    /**
     * Given any root-level path for the given UpdateSource (public key, version manifest, etc), this
     * method will return the actual File within the current project's dist subdirectory that
     * represents that path.
     * <p>
     * No check is done that the file actually exists! This is where the File <i>should</i> be,
     * not where it necessarily is.
     * </p>
     * <p>
     *     For extension-specific files, such as jar download URLs or screenshots, use
     *     getProjectFileFromPath(ExtensionVersion, String) instead.
     * </p>
     * <p>
     *     If no project is currently open, the result is null.
     * </p>
     * <p><b>EXAMPLE:</b></p>
     * <pre>getProjectFileFromPath("public.key"); // returns /home/you/yourProjectDir/public.key</pre>
     */
    public File getProjectFileFromPath(String path) {
        return getProjectFileFromPath(null, path);
    }

    /**
     * Given any extension-specific path (jar download, screenshot, signature file, etc), this
     * method will return the actual File within the project's dist directory structure that
     * represents that path.
     * <p>
     * No check is done that the file actually exists! This is where the File <i>should</i> be,
     * not where it necessarily is.
     * </p>
     * <p>
     * For top-level project files, such as public key or version manifest, use
     * getProjectFileFromPath(String) instead, or you can specify null
     * for the ExtensionVersion.
     * </p>
     * <p>
     * If no project is currently open, the result is null.
     * </p>
     * <P><b>EXAMPLE:</b></P>
     * <PRE>
     * getProjectFileFromPath(myExtensionVersion, "MyExtension-1.0.0.jar");
     * // This returns "/home/you/yourProjectDir/extensions/1.0/MyExtension-1.0.0.jar"
     * // (we infer the directory structure based on the target app version of the extension)
     * </PRE>
     */
    public File getProjectFileFromPath(ExtensionVersion extensionVersion, String path) {
        File containingDir = extensionVersion == null
                ? project.getDistDir()
                : new File(project.getDistDir(), "extensions/" + extensionVersion.getExtInfo().getTargetAppVersion());
        return new File(containingDir, path);
    }

    /**
     * Returns the given filename without its extension (if it has an extension, otherwise as-is).
     */
    public static String getBasename(String filename) {
        if (filename == null || filename.isBlank()) {
            return filename;
        }
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename;
        }
        return filename.substring(0, index);
    }

    public List<File> findAllJars(Project project) {
        return project == null ? List.of() : FileSystemUtil.findFiles(project.getDistDir(), true, "jar");
    }

    public List<File> findAllSignatures(Project project) {
        return project == null ? List.of() : FileSystemUtil.findFiles(project.getDistDir(), true, "sig");
    }

    private boolean isImageFile(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith("gif");
    }

    /**
     * Callers can register to receive notification when a project is loaded or saved.
     */
    public void addProjectListener(ProjectListener listener) {
        projectListeners.add(listener);
    }

    public void removeProjectListener(ProjectListener listener) {
        projectListeners.remove(listener);
    }

    private void fireProjectLoadedEvent(Project project) {
        List<ProjectListener> copy = new ArrayList<>(projectListeners);
        for (ProjectListener listener : copy) {
            listener.projectLoaded(project);
        }
    }

    private void fireProjectSavedEvent(Project project) {
        List<ProjectListener> copy = new ArrayList<>(projectListeners);
        for (ProjectListener listener : copy) {
            listener.projectSaved(project);
        }
    }
}
