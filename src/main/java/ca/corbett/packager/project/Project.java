package ca.corbett.packager.project;

import ca.corbett.extras.crypt.SignatureUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.properties.FileBasedProperties;
import ca.corbett.updates.UpdateSources;
import ca.corbett.updates.VersionManifest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents all the settings for a saved project in the ExtPackager application.
 * A Project here in this application maps 1:1 to an application that you want to distribute.
 * So, a good convention is to name the project after the application in question,
 * but this is not enforced.
 * <p>
 *     The singleton ProjectManager class provides handy wrappers around this stuff so that
 *     callers can always get access to the currently loaded Project.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Project {

    private static final Logger log = Logger.getLogger(Project.class.getName());

    private String name;
    private final FileBasedProperties props;
    private final File projectDir;
    private final File distDir;
    private final File extensionsDir;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private UpdateSources updateSources;
    private VersionManifest versionManifest;

    private final Gson gson;

    private Project(String name, FileBasedProperties props) {
        this(name, props, null, null);
    }

    private Project(String name, FileBasedProperties props, PublicKey publicKey, PrivateKey privateKey) {
        this.name = name;
        this.props = props;
        this.projectDir = props.getFile().getParentFile();
        this.distDir = new File(projectDir, "dist");
        this.extensionsDir = new File(projectDir, "extensions");
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.updateSources = new UpdateSources(name);
        this.versionManifest = new VersionManifest();
        versionManifest.setApplicationName(name);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Returns the name of this ext-packager Project.
     * By convention, this should match the application name, but this is not enforced.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name for this ext-packager Project. Will overwrite any previous name.
     */
    public void setName(String name) {
        this.name = name;
        props.setString("projectName", name);
        props.saveWithoutException();
    }

    /**
     * Returns the containing directory for this Project.
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * Returns the distribution dir for this project (this is projectDir/dist).
     */
    public File getDistDir() {
        return distDir;
    }

    /**
     * Returns the extensions dir for this project (this is projectDir/extensions).
     */
    public File getExtensionsDir() {
        return extensionsDir;
    }

    /**
     * Returns the PrivateKey for this Project, or null if no key pair is set.
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Returns the File representing the private key for this project. Note that this file may
     * or may not exist, depending on whether a key pair has been generated for this Project.
     * This method will always return a File object, representing the location where the
     * private key would be stored if it exists even if no key pair has been generated.
     */
    public File getPrivateKeyFile() {
        return new File(projectDir, "private.key");
    }

    /**
     * Returns the PublicKey for this Project, or null if no key pair is set.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Returns the File representing the public key for this project. Note that this file may
     * or may not exist, depending on whether a key pair has been generated for this Project.
     * This method will always return a File object, representing the location where the
     * public key would be stored if it exists even if no key pair has been generated.
     */
    public File getPublicKeyFile() {
        return new File(distDir, "public.key");
    }

    /**
     * Sets a new KeyPair for this Project, overwriting any previous public and private key.
     */
    public void setKeyPair(KeyPair keyPair) throws IOException {
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    /**
     * Returns the UpdateSources for this Project.
     */
    public UpdateSources getUpdateSources() {
        return updateSources;
    }

    /**
     * Returns the File to which the UpdateSources for this project will be saved.
     */
    public File getUpdateSourcesFile() {
        return new File(projectDir, "update_sources.json");
    }

    /**
     * Sets the UpdateSources for this Project.
     */
    public void setUpdateSources(UpdateSources updateSources) {
        this.updateSources = updateSources;
    }

    /**
     * Returns the VersionManifest for this Project.
     */
    public VersionManifest getVersionManifest() {
        return versionManifest;
    }

    /**
     * Returns the File to which the VersionManifest for this project will be saved.
     */
    public File getVersionManifestFile() {
        return new File(distDir, "version_manifest.json");
    }

    /**
     * Sets the VersionManifest for this Project.
     */
    public void setVersionManifest(VersionManifest manifest) {
        this.versionManifest = manifest;
    }

    /**
     * Generates json for the UpdateSources and VersionManifest for this Project and
     * saves them to the appropriate locations. Also saves the current public and private
     * key pair, if they are set.
     */
    public void save() throws IOException {
        if (privateKey != null) {
            SignatureUtil.savePrivateKey(privateKey, getPrivateKeyFile());
        }
        if (publicKey != null) {
            SignatureUtil.savePublicKey(publicKey, getPublicKeyFile());
        }
        if (updateSources != null) {
            FileSystemUtil.writeStringToFile(gson.toJson(updateSources), getUpdateSourcesFile());
        }
        if (versionManifest != null) {
            FileSystemUtil.writeStringToFile(gson.toJson(versionManifest), getVersionManifestFile());
        }
    }

    /**
     * Creates a new, empty Project in the given project directory and with the given name.
     * A properties file will be created for the project, and the distribution directory
     * will be created automatically if it does not already exist.
     */
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
        File extensionsDir = new File(projectDir, "extensions");
        if (!extensionsDir.exists()) {
            if (!extensionsDir.mkdirs()) {
                throw new IOException("Unable to create extensions directory in this location.");
            }
        }
        if (extensionsDir.exists() && !extensionsDir.isDirectory()) {
            throw new IOException("Extensions directory is corrupt in this location");
        }
        FileBasedProperties props = new FileBasedProperties(new File(projectDir, name + ".extpkg"));
        props.setString("projectName", name);
        props.save();
        return new Project(name, props);
    }

    /**
     * Attempts to load an ext-packager Project from the given project file.
     * If any required files are missing, an IOException is thrown.
     */
    public static Project fromFile(File projectFile) throws IOException {
        File projectDir = projectFile.getParentFile();
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            throw new IOException("Unable to find the distribution directory in this location.");
        }
        File extensionsDir = new File(projectDir, "extensions");
        if (!extensionsDir.exists() || !extensionsDir.isDirectory()) {
            throw new IOException("Unable to find the extensions directory in this location.");
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
        project.loadVersionManifest();

        return project;
    }

    /**
     * Invoked internally to load the UpdateSources list for this Project.
     */
    private void loadUpdateSources() {
        // Start by blanking out our current UpdateSources in case the load fails:
        updateSources = new UpdateSources(name);

        // Now try to load from disk:
        File updateSourcesFile = getUpdateSourcesFile();
        if (updateSourcesFile.exists()) {
            try {
                updateSources = gson.fromJson(FileSystemUtil.readFileToString(updateSourcesFile), UpdateSources.class);
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "Problem reading update_sources.json: " + ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Invoked internally to load the version manifest for this Project.
     */
    private void loadVersionManifest() {
        // Start by blanking out our current VersionManifest in case the load fails:
        versionManifest = new VersionManifest();
        versionManifest.setApplicationName(name);

        // Now try to load from disk:
        File manifestFile = getVersionManifestFile();
        if (manifestFile.exists()) {
            try {
                versionManifest = gson.fromJson(FileSystemUtil.readFileToString(manifestFile), VersionManifest.class);
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "Problem reading version_manifest.json: " + ioe.getMessage(), ioe);
            }
        }
    }
}
