package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.extras.progress.SimpleProgressAdapter;
import ca.corbett.extras.properties.Properties;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.PasswordField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.packager.AppConfig;
import ca.corbett.packager.io.FileSystemUploadThread;
import ca.corbett.packager.io.FtpParams;
import ca.corbett.packager.io.FtpUploadThread;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.updates.UpdateSources;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides a means to upload all files to a given UpdateSource, either by ftp or
 * by local filesystem copies, depending on the source.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class UploadCard extends JPanel implements ProjectListener {

    private static final Logger log = Logger.getLogger(UploadCard.class.getName());

    private MessageUtil messageUtil;
    private final FormPanel formPanel;
    private final CheckValidator keyPairValidator;
    private final CheckValidator updateSourceValidator;
    private final CheckValidator jarsPresentValidator;
    private final CheckValidator jarsSignedValidator;
    private final ComboField<String> sourceCombo;
    private final LabelField targetDirField;
    private final ShortTextField ftpHostField;
    private final ShortTextField ftpUsernameField;
    private final PasswordField ftpPasswordField;
    private final ShortTextField ftpTargetDirField;
    private final CheckBoxField ftpSaveParamsCheckbox;
    private final CheckBoxField cleanDirBeforeUpload;

    public UploadCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Upload", 20));

        keyPairValidator = new CheckValidator();
        updateSourceValidator = new CheckValidator();
        jarsPresentValidator = new CheckValidator();
        jarsSignedValidator = new CheckValidator();

        formPanel.add(new LabelField("Project defines a key pair")
                              .addFieldValidator(keyPairValidator));
        formPanel.add(new LabelField("Project has at least one update source")
                              .addFieldValidator(updateSourceValidator));
        formPanel.add(new LabelField("Project has at least one extension jar")
                              .addFieldValidator(jarsPresentValidator));
        formPanel.add(new LabelField("All extension jars are signed")
                              .addFieldValidator(jarsSignedValidator));

        sourceCombo = new ComboField<>("Upload to:");
        sourceCombo.getMargins().setTop(24);
        sourceCombo.addValueChangedListener(field -> comboValueChanged());
        formPanel.add(sourceCombo);

        // Controls specific to file-based upload sources:
        targetDirField = new LabelField("Target dir:", "");
        targetDirField.setVisible(false);
        formPanel.add(targetDirField);

        // Controls specific to ftp-based upload sources:
        Properties props = AppConfig.getInstance().getPropertiesManager().getPropertiesInstance();
        ftpHostField = new ShortTextField("Host:", 15);
        ftpHostField.setVisible(false);
        ftpHostField.setAllowBlank(false);
        ftpHostField.setText(props.getString("ftpHost", ""));
        ftpUsernameField = new ShortTextField("Username:", 15);
        ftpUsernameField.setVisible(false);
        ftpUsernameField.setAllowBlank(false);
        ftpUsernameField.setText(props.getString("ftpUser", ""));
        ftpPasswordField = new PasswordField("Password:", 15);
        ftpPasswordField.setVisible(false);
        ftpPasswordField.setAllowBlank(false);
        ftpPasswordField.setPassword(props.getString("ftpPass", ""));
        ftpTargetDirField = new ShortTextField("Target dir:", 15);
        ftpTargetDirField.setVisible(false);
        ftpTargetDirField.setAllowBlank(false);
        ftpTargetDirField.setText(props.getString("ftpDir", ""));
        ftpSaveParamsCheckbox = new CheckBoxField("Save FTP parameters", true);
        ftpSaveParamsCheckbox.setVisible(false);
        cleanDirBeforeUpload = new CheckBoxField("Clean FTP directory before upload", true);
        formPanel.add(ftpHostField);
        formPanel.add(ftpUsernameField);
        formPanel.add(ftpPasswordField);
        formPanel.add(ftpTargetDirField);
        formPanel.add(ftpSaveParamsCheckbox);
        formPanel.add(cleanDirBeforeUpload);

        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Upload");
        button.setPreferredSize(new Dimension(90, 24));
        button.addActionListener(e -> doUpload());
        panelField.getPanel().add(button);
        formPanel.add(panelField);

        add(formPanel, BorderLayout.CENTER);

        ProjectManager.getInstance().addProjectListener(this);
    }

    private void comboValueChanged() {
        setFileUploadControlsVisible(false);
        setFtpUploadControlsVisible(false);

        Project project = ProjectManager.getInstance().getProject();
        int selectedIndex = sourceCombo.getSelectedIndex();
        if (project == null || selectedIndex == -1) {
            return;
        }

        UpdateSources.UpdateSource updateSource = project.getUpdateSources().getUpdateSources().get(selectedIndex);
        if (updateSource.getBaseUrl().getProtocol().equalsIgnoreCase("file")) {
            setFileUploadControlsVisible(true);
            try {
                targetDirField.setText(new File(updateSource.getBaseUrl().toURI()).getAbsolutePath());
            }
            catch (URISyntaxException | IllegalArgumentException e) {
                targetDirField.setText("(invalid base URL)");
            }
        }
        else {
            setFtpUploadControlsVisible(true);
        }

        formPanel.validateForm(); // revalidate form as visible controls may have changed.
    }

    private void setFileUploadControlsVisible(boolean visible) {
        targetDirField.setVisible(visible);
    }

    private void setFtpUploadControlsVisible(boolean visible) {
        ftpHostField.setVisible(visible);
        ftpUsernameField.setVisible(visible);
        ftpPasswordField.setVisible(visible);
        ftpTargetDirField.setVisible(visible);
        ftpSaveParamsCheckbox.setVisible(visible);
    }

    private void doUpload() {
        Project project = ProjectManager.getInstance().getProject();

        if (project == null) {
            getMessageUtil().info("No project is loaded.");
            return;
        }

        List<UpdateSources.UpdateSource> updateSources = project.getUpdateSources().getUpdateSources();
        if (updateSources.isEmpty()) {
            getMessageUtil().info("Project defines no update sources.");
            return;
        }

        // Some warnings, like the project not having a key pair or not all jars being signed,
        // might be intentional and are not necessarily fatal. Still, give user a chance to think it over:
        if (!formPanel.isFormValid()) {
            if (JOptionPane.showConfirmDialog(MainWindow.getInstance(),
                                              "Proceed despite warnings?",
                                              "Confirm",
                                              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        UpdateSources.UpdateSource updateSource = updateSources.get(sourceCombo.getSelectedIndex());
        if (updateSource.getBaseUrl().getProtocol().equalsIgnoreCase("file")) {
            FileSystemUploadThread worker = new FileSystemUploadThread(project,
                                                                       new File(targetDirField.getText()),
                                                                       cleanDirBeforeUpload.isChecked());
            worker.addProgressListener(new UploadProgressListener());
            new MultiProgressDialog(MainWindow.getInstance(), "Filesystem upload")
                    .runWorker(worker, true);
        }
        else {
            FtpUploadThread worker = new FtpUploadThread(project,
                                                         updateSource,
                                                         buildFtpParams(),
                                                         cleanDirBeforeUpload.isChecked());
            worker.addProgressListener(new UploadProgressListener());
            new MultiProgressDialog(MainWindow.getInstance(), "FTP upload")
                    .runWorker(worker, true);
        }
    }

    private FtpParams buildFtpParams() {
        FtpParams params = new FtpParams();
        params.host = ftpHostField.getText();
        params.username = ftpUsernameField.getText();
        params.targetDir = ftpTargetDirField.getText();
        params.password = ftpPasswordField.getPassword();

        // Save all ftp props if directed, or blank them out otherwise:
        Properties props = AppConfig.getInstance().getPropertiesManager().getPropertiesInstance();
        props.setString("ftpHost", ftpSaveParamsCheckbox.isChecked() ? ftpHostField.getText() : "");
        props.setString("ftpUser", ftpSaveParamsCheckbox.isChecked() ? ftpUsernameField.getText() : "");
        props.setString("ftpDir", ftpSaveParamsCheckbox.isChecked() ? ftpTargetDirField.getText() : "");
        props.setString("ftpPass", ftpSaveParamsCheckbox.isChecked() ? ftpPasswordField.getPassword() : "");
        AppConfig.getInstance().save();

        return params;
    }

    private void validateForm() {
        Project project = ProjectManager.getInstance().getProject();

        if (project == null) {
            setAllValidation("No project loaded.");
            formPanel.validateForm();
            return;
        }

        setAllValidation(null);
        sourceCombo.getComboModel().removeAllElements();

        if (project.getPrivateKey() == null || project.getPublicKey() == null) {
            keyPairValidator.setMessage("Project has no key pair - unable to sign.");
        }
        if (project.getUpdateSources() == null || project.getUpdateSources().getUpdateSources().isEmpty()) {
            updateSourceValidator.setMessage("Project has no update source defined.");
        }
        else {
            for (UpdateSources.UpdateSource source : project.getUpdateSources().getUpdateSources()) {
                sourceCombo.getComboModel().addElement(source.getName());
            }
        }
        List<File> jarFiles = ProjectManager.getInstance().findAllJars(project);
        if (jarFiles.isEmpty()) {
            jarsPresentValidator.setMessage("Project contains no extension jars.");
        }
        List<File> sigFiles = ProjectManager.getInstance().findAllSignatures(project);
        if (sigFiles.isEmpty()) {
            jarsSignedValidator.setMessage("No jar signatures detected.");
        }
        else if (sigFiles.size() != jarFiles.size()) {
            jarsSignedValidator.setMessage(
                    "Only " + sigFiles.size() + " jars of " + jarFiles.size() + " have been signed.");
        }
        else {
            jarsSignedValidator.setMessage(null);
        }
        formPanel.validateForm();
    }

    private void setAllValidation(String message) {
        keyPairValidator.setMessage(message);
        updateSourceValidator.setMessage(message);
        jarsPresentValidator.setMessage(message);
        jarsSignedValidator.setMessage(message);
    }

    @Override
    public void projectLoaded(Project project) {
        validateForm();
    }

    @Override
    public void projectSaved(Project project) {
        validateForm();
    }

    private static class CheckValidator implements FieldValidator<FormField> {

        private String message;

        public CheckValidator() {
            message = null;
        }

        public CheckValidator(String message) {
            this.message = message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public ValidationResult validate(FormField fieldToValidate) {
            return message == null ? ValidationResult.valid() : ValidationResult.invalid(message);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }

    private class UploadProgressListener extends SimpleProgressAdapter {
        @Override
        public boolean progressError(String errorSource, String errorDetails) {
            getMessageUtil().error(errorSource, errorDetails);
            return false;
        }

        @Override
        public void progressComplete() {
            getMessageUtil().info("Upload complete!");
        }
    }
}
