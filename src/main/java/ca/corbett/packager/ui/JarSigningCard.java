package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.crypt.SignatureUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.dialogs.SignatureChoiceDialog;
import ca.corbett.updates.VersionManifest;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This card provides a way to cryptographically sign all extension jars prior to upload,
 * and also to verify signatures that have previously been generated.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class JarSigningCard extends JPanel implements ProjectListener {

    private static final Logger log = Logger.getLogger(JarSigningCard.class.getName());

    private MessageUtil messageUtil;
    private final LabelField statusLabel;

    public JarSigningCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Jar signing", 20));

        statusLabel = new LabelField("Status:", "Not yet scanned.");
        formPanel.add(statusLabel);
        formPanel.add(buildButtonField());

        add(formPanel, BorderLayout.CENTER);

        ProjectManager.getInstance().addProjectListener(this);
    }

    private void scanAndVerify() {
        Project project = ProjectManager.getInstance().getProject();
        if (project == null) {
            getMessageUtil().info("No project is open.");
            return;
        }

        if (project.getPublicKey() == null || project.getPrivateKey() == null) {
            getMessageUtil().info("This project has no key pair defined.");
            return;
        }

        List<File> jarFiles = ProjectManager.getInstance().findAllJars(project);
        if (jarFiles.isEmpty()) {
            getMessageUtil().info("This project has no extension jars.");
            return;
        }

        // For each jar, verify its signature if it is signed:
        int countSigned = 0;
        int countVerified = 0;
        for (File jarFile : jarFiles) {
            File sigFile = new File(jarFile.getParentFile(), ProjectManager.getBasename(jarFile.getName()) + ".sig");
            if (sigFile.exists()) {
                countSigned++;
                if (isSignatureValid(jarFile, sigFile, project)) {
                    log.info("Jar file " + jarFile.getAbsolutePath() + " signature verified.");
                    countVerified++;
                }
                else {
                    log.warning("Jar file " + jarFile.getAbsolutePath() + " signature verification failed!");
                }
            }
        }

        // Display results:
        if (countSigned == 0) {
            getMessageUtil().info("No jars in this project are signed.");
            return;
        }
        if (jarFiles.size() > countSigned) {
            getMessageUtil().info("Some jars in this project are not signed.");
            return;
        }
        if (countSigned == countVerified) {
            getMessageUtil().info("All jars signed and verified!");
            return;
        }
        getMessageUtil().warning("Not all jars in this project could be verified!\n"
                                         + "Has the key pair for this project been regenerated recently?\n"
                                         + "Recommended action: re-sign all jar files!");
    }

    /**
     * Invoked internally to prompt the user as to how exactly signing should be done, then
     * actually do it.
     */
    private void signJars() {
        Project project = ProjectManager.getInstance().getProject();
        if (project == null) {
            getMessageUtil().info("No project is currently open.");
            return;
        }

        if (project.getPublicKey() == null || project.getPrivateKey() == null) {
            getMessageUtil().info("This project has no key pair defined - unable to sign or verify.");
            return;
        }

        List<File> jarFiles = ProjectManager.getInstance().findAllJars(project);
        if (jarFiles.isEmpty()) {
            getMessageUtil().info("This project has no extension jars.");
            return;
        }

        SignatureChoiceDialog dialog = new SignatureChoiceDialog();
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }
        SignatureChoiceDialog.Choice sigType = dialog.getChoice();

        int countSigned = 0;
        for (File jarFile : jarFiles) {
            File sigFile = new File(jarFile.getParentFile(),
                                    ProjectManager.getBasename(jarFile.getName()) + ".sig");

            if (shouldSignJar(jarFile, sigFile, sigType, project)) {
                log.info("Signing jar file " + jarFile.getAbsolutePath());
                signJar(jarFile, sigFile, project);
                countSigned++;
            }
            else {
                log.info("Skipping jar file " + jarFile.getAbsolutePath());
            }
        }

        reset(); // Force rescan and redisplay.
        if (countSigned == 0) {
            getMessageUtil().info("Signing complete - no jars were signed.");
        }
        else {
            getMessageUtil().info(countSigned + " extension jars were signed.");
            try {
                project.save();
            }
            catch (IOException ioe) {
                getMessageUtil().error("Unable to save project! Error: " + ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Invoked internally to decide whether the given jar file needs to be signed (or re-signed)
     * depending on the given signature choice and the given signature file, which may or may
     * not already exist.
     */
    private boolean shouldSignJar(File jarFile, File sigFile,
                                  SignatureChoiceDialog.Choice sigType, Project project) {
        boolean sigExists = sigFile.exists();

        return switch (sigType) {
            case SIGN_EVERYTHING -> deleteSignatureIfExists(sigFile);
            case SIGN_MISSING -> !sigExists;
            case SIGN_MISSING_OR_FAILED -> {
                if (!sigExists) {
                    yield true;
                }
                yield (!isSignatureValid(jarFile, sigFile, project)) && deleteSignatureIfExists(sigFile);
            }
        };
    }

    /**
     * Deletes the given signature file if it exists, and returns true if successful (or if the file
     * didn't exist).
     */
    private boolean deleteSignatureIfExists(File sigFile) {
        if (!sigFile.exists()) {
            return true;
        }

        try {
            Files.delete(sigFile.toPath());
            return true;
        }
        catch (IOException ioe) {
            getMessageUtil().error(
                    "Unable to delete existing signature file " + sigFile.getAbsolutePath() + ": " + ioe.getMessage(),
                    ioe);
            return false;
        }
    }

    /**
     * Checks the signature on the given jar file and returns true if it validates properly.
     */
    private boolean isSignatureValid(File jarFile, File sigFile, Project project) {
        try {
            return SignatureUtil.verifyFile(jarFile, sigFile, project.getPublicKey());
        }
        catch (Exception e) {
            getMessageUtil().error(
                    "Unable to verify signature for jar " + jarFile.getAbsolutePath() + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Signs the given jar file using the given signature.
     * Also updates the given project with the new signature file reference.
     */
    private void signJar(File jarFile, File sigFile, Project project) {
        try {
            SignatureUtil.signFile(jarFile, project.getPrivateKey(), sigFile);
            VersionManifest.ExtensionVersion extVersion = findExtensionVersionFromJar(project, jarFile);
            if (extVersion != null) {
                extVersion.setSignaturePath(sigFile.getName());
            }
            else {
                log.warning("Unable to find extension version matching jar " + jarFile.getAbsolutePath());
            }
        }
        catch (Exception e) {
            getMessageUtil().error("Unable to sign jar " + jarFile.getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Given a jar file, searches through the given project's manifest and returns the ExtensionVersion
     * that owns that jar. Will return null if no owning ExtensionVersion is found.
     */
    private VersionManifest.ExtensionVersion findExtensionVersionFromJar(Project project, File jarFile) {
        for (VersionManifest.ApplicationVersion appVersion : project.getVersionManifest().getApplicationVersions()) {
            for (VersionManifest.Extension extension : appVersion.getExtensions()) {
                for (VersionManifest.ExtensionVersion candidateVersion : extension.getVersions()) {
                    if (jarFile.getName().equals(candidateVersion.getDownloadPath())) {
                        return candidateVersion;
                    }
                }
            }
        }
        return null;
    }

    private FormField buildButtonField() {
        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Scan and verify");
        button.setPreferredSize(new Dimension(130, 24));
        button.addActionListener(e -> scanAndVerify());
        panelField.getPanel().add(button);
        button = new JButton("Sign all jars");
        button.setPreferredSize(new Dimension(130, 24));
        button.addActionListener(e -> signJars());
        panelField.getPanel().add(button);
        return panelField;
    }

    private void reset() {
        statusLabel.setText("Not yet scanned.");

        Project project = ProjectManager.getInstance().getProject();
        if (project == null) {
            return;
        }

        List<File> jarFiles = ProjectManager.getInstance().findAllJars(project);
        List<File> sigFiles = new ArrayList<>();
        for (File f : jarFiles) {
            String basename = ProjectManager.getBasename(f.getName());
            File sigFile = new File(f.getParentFile(), basename + ".sig");
            if (sigFile.exists()) {
                sigFiles.add(sigFile);
            }
        }
        statusLabel.setText(jarFiles.size() + " jar files present; " + sigFiles.size() + " are signed.");
    }

    @Override
    public void projectLoaded(Project project) {
        reset();
    }

    @Override
    public void projectSaved(Project project) {
        reset();
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
