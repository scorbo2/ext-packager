package ca.corbett.packager.project;

public interface ProjectListener {

    /**
     * The given Project has just been loaded and is now the current Project.
     */
    void projectLoaded(Project project);

    /**
     * The given Project has just been saved the disk contents have perhaps changed.
     */
    void projectSaved(Project project);
}
