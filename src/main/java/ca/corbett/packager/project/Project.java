package ca.corbett.packager.project;

import ca.corbett.extras.properties.FileBasedProperties;

import java.io.File;

/**
 * Represents all the settings for a saved project in the ExtPackager application.
 * A Project here in this application maps 1:1 to an application that you want to distribute.
 * So, a good convention is to name the project after the application in question.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Project {
    private String name;
    private final File projectDir;
    private final FileBasedProperties props;
    private final File distDir;

    public Project(String name, File projectDir) {
        this.name = name;
        this.projectDir = projectDir;
        this.distDir = new File(projectDir, "dist");
        distDir.mkdirs();
        props = new FileBasedProperties(new File(projectDir, "settings.extpkg"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getProjectDir() {
        return projectDir;
    }
}
