package ca.corbett.packager.project;

import ca.corbett.extras.CoalescingDocumentListener;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.packager.ui.MainWindow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        project = Project.fromFile(projectFile);
        MainWindow.getInstance().projectOpened();
        fireProjectLoadedEvent(project);
    }

    /**
     * Saves any changes to the current Project, if one is open.
     */
    public void save() throws IOException {
        if (project == null || isSaveInProgress) {
            return;
        }
        isSaveInProgress = true;
        try {
            project.save();
            fireProjectSavedEvent(project);
        }
        finally {
            // Extremely cheesy, but we have to wait for coalescing doc listeners to finish responding.
            new Thread(() -> {
                try {
                    Thread.sleep(CoalescingDocumentListener.DELAY_MS * 2); // cheesy!
                    isSaveInProgress = false;
                }
                catch (Exception ignored) { }
            }).start();
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

    /**
     * Callers can register to receive notification when a project is loaded.
     */
    public void addProjectListener(ProjectListener listener) {
        projectListeners.add(listener);
    }

    public void removeProjectListener(ProjectListener listener) {
        projectListeners.remove(listener);
    }

    private void fireProjectLoadedEvent(Project project) {
        for (ProjectListener listener : projectListeners) {
            listener.projectLoaded(project);
        }
    }

    private void fireProjectSavedEvent(Project project) {
        for (ProjectListener listener : projectListeners) {
            listener.projectSaved(project);
        }
    }
}
