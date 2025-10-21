package ca.corbett.packager.project;

import ca.corbett.extras.crypt.SignatureUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.properties.FileBasedProperties;
import ca.corbett.updates.UpdateSources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents all the settings for a saved project in the ExtPackager application.
 * A Project here in this application maps 1:1 to an application that you want to distribute.
 * So, a good convention is to name the project after the application in question.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Project {

    private static final Logger log = Logger.getLogger(Project.class.getName());

    private String name;
    private final FileBasedProperties props;
    private final File projectDir;
    private final File distDir;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private UpdateSources updateSources;

    private final Gson gson;

    private Project(String name, FileBasedProperties props, PublicKey publicKey, PrivateKey privateKey) {
        this.name = name;
        this.props = props;
        this.projectDir = props.getFile().getParentFile();
        this.distDir = new File(props.getFile().getParentFile(), "dist");
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) throws IOException {
        this.privateKey = privateKey;
        File privateKeyFile = new File(projectDir, "private.key");

        // Setting null is a wonky case, but if we ever get it, consider it a "delete" request:
        if (privateKey == null) {
            if (privateKeyFile.exists()) {
                privateKeyFile.delete();
            }
            return;
        }

        // Otherwise (key is not null), save it:
        SignatureUtil.savePrivateKey(privateKey, privateKeyFile);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) throws IOException {
        this.publicKey = publicKey;
        File publicKeyFile = new File(distDir, "public.key");

        // Setting null is a wonky case, but if we ever get it, consider it a "delete" request:
        if (publicKey == null) {
            if (publicKeyFile.exists()) {
                publicKeyFile.delete();
            }
            return;
        }

        // Otherwise (key is not null), save it:
        SignatureUtil.savePublicKey(publicKey, publicKeyFile);
    }

    public UpdateSources getUpdateSources() {
        return updateSources;
    }

    public void loadUpdateSources() {
        updateSources = null;
        File updateSourcesFile = new File(projectDir, "update_sources.json");
        if (updateSourcesFile.exists()) {
            try {
                updateSources = gson.fromJson(FileSystemUtil.readFileToString(updateSourcesFile), UpdateSources.class);
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "Problem reading update_sources.json: " + ioe.getMessage(), ioe);
            }
        }
    }

    public void setUpdateSources(UpdateSources updateSources) {
        this.updateSources = updateSources;
        File updateSourcesFile = new File(projectDir, "update_sources.json");
        if (updateSources == null) {
            if (updateSourcesFile.exists()) {
                updateSourcesFile.delete();
            }
            return;
        }
        try {
            FileSystemUtil.writeStringToFile(gson.toJson(updateSources), updateSourcesFile);
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Problem writing update_sources.json: " + ioe.getMessage(), ioe);
        }
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
        return new Project(name, props, null, null);
    }

    public static Project fromFile(File projectFile) throws IOException {
        File projectDir = projectFile.getParentFile();
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            throw new IOException("Unable to find the distribution directory in this location.");
        }
        FileBasedProperties props = new FileBasedProperties(projectFile);
        props.load();
        String name = props.getString("projectName", "");
        if ("".equals(name)) {
            throw new IOException("Project file appears corrupt.");
        }

        PublicKey publicKey = null;
        File publicKeyFile = new File(distDir, "public.key");
        if (publicKeyFile.exists()) {
            try {
                publicKey = SignatureUtil.loadPublicKey(publicKeyFile);
            }
            catch (Exception e) {
                throw new IOException("Unable to load public key: " + e.getMessage(), e);
            }
        }

        PrivateKey privateKey = null;
        File privateKeyFile = new File(projectDir, "private.key");
        if (privateKeyFile.exists()) {
            try {
                privateKey = SignatureUtil.loadPrivateKey(privateKeyFile);
            }
            catch (Exception e) {
                throw new IOException("Unable to load private key: " + e.getMessage(), e);
            }
        }

        Project project = new Project(name, props, publicKey, privateKey);
        project.loadUpdateSources();

        return project;
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

    public File getDistDir() {
        return distDir;
    }
}
