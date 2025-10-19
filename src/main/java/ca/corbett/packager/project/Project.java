package ca.corbett.packager.project;

import ca.corbett.extras.properties.FileBasedProperties;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents all the settings for a saved project in the ExtPackager application.
 * A Project here in this application maps 1:1 to an application that you want to distribute.
 * So, a good convention is to name the project after the application in question.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Project {

    private final Logger log = Logger.getLogger(Project.class.getName());

    private String name;
    private final FileBasedProperties props;
    private final File distDir;

    private Project(String name, FileBasedProperties props) {
        this.name = name;
        this.props = props;
        this.distDir = new File(props.getFile().getParentFile(), "dist");
    }

    public static Project createNew(String name, File projectDir) throws IOException {
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            if (!distDir.mkdirs()) {
                throw new IOException("Unable to create distribution directory in this location.");
            }
        }
        if (distDir.exists() && !distDir.isDirectory()) {
            throw new IOException("Distribution directory is corrupt in this location.");
        }
        FileBasedProperties props = new FileBasedProperties(new File(projectDir, name + ".extpkg"));
        props.setString("projectName", name);
        props.save();
        return new Project(name, props);
    }

    public static Project fromFile(File projectFile) throws IOException {
        File distDir = new File(projectFile.getParentFile(), "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            throw new IOException("Unable to find the distribution directory in this location.");
        }
        FileBasedProperties props = new FileBasedProperties(projectFile);
        props.load();
        String name = props.getString("projectName", "");
        if ("".equals(name)) {
            throw new IOException("Project file appears corrupt.");
        }
        return new Project(name, props);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getProjectDir() {
        return props.getFile().getParentFile();
    }
}
