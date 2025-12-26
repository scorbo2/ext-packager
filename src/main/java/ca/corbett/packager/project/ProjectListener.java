package ca.corbett.packager.project;

/**
 * Listener interface for receiving project-related events from a ProjectManager instance.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public interface ProjectListener {

    /**
     * The given Project is about to be loaded and will become the current Project.
     */
    void projectWillLoad(Project project);

    /**
     * The given Project has just been loaded and is now the current Project.
     */
    void projectLoaded(Project project);

    /**
     * The given Project has just been saved the disk contents have perhaps changed.
     */
    void projectSaved(Project project);

    /**
     * The given Project has been closed and is no longer the current Project.
     */
    void projectClosed(Project project);
}
