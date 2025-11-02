package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.updates.UpdateSources;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
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
        formPanel.add(sourceCombo);

        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Upload");
        button.setPreferredSize(new Dimension(90, 24));
        button.addActionListener(e -> doUpload());
        panelField.getPanel().add(button);
        formPanel.add(panelField);

        add(formPanel, BorderLayout.CENTER);

        ProjectManager.getInstance().addProjectListener(this);
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

        UpdateSources.UpdateSource updateSource = updateSources.get(sourceCombo.getSelectedIndex());
        // TODO hand this updateSource to an UploadDialog and go...
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
        if (sigFiles.isEmpty() || sigFiles.size() != jarFiles.size()) {
            jarsSignedValidator.setMessage("Not all jars have been signed.");
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
}
