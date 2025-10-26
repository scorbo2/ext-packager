package ca.corbett.packager.ui.dialogs;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.packager.ui.MainWindow;
import ca.corbett.updates.VersionManifest;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class ApplicationVersionDialog extends JDialog {

    private boolean wasOkayed;
    private FormPanel formPanel;
    private LabelField appNameField;
    private ShortTextField versionField;
    private UniquenessChecker uniquenessChecker;
    private final String applicationName;

    public ApplicationVersionDialog(String title, String appName) {
        super(MainWindow.getInstance(), title, true);
        this.applicationName = appName;
        setSize(new Dimension(550, 480));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setLayout(new BorderLayout());
        add(PropertiesDialog.buildScrollPane(buildFormPanel()), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    public boolean wasOkayed() {
        return wasOkayed;
    }

    public void setUniquenessChecker(UniquenessChecker uniquenessChecker) {
        this.uniquenessChecker = uniquenessChecker;
    }

    public void setApplicationVersion(VersionManifest.ApplicationVersion appVersion) {
        versionField.setText(appVersion.getVersion());
    }

    public VersionManifest.ApplicationVersion getApplicationVersion() {
        if (!wasOkayed) {
            return null;
        }

        VersionManifest.ApplicationVersion appVersion = new VersionManifest.ApplicationVersion();
        appVersion.setVersion(versionField.getText());
        // TODO populate extensions and extension versions and screenshots, oh my
        return appVersion;
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        appNameField = new LabelField("Application name:", applicationName);
        formPanel.add(appNameField);

        versionField = new ShortTextField("Application version:", 10);
        versionField.setAllowBlank(false);
        versionField.addFieldValidator(new FieldValidator<ShortTextField>() {
            @Override
            public ValidationResult validate(ShortTextField fieldToValidate) {
                if (uniquenessChecker == null) {
                    return ValidationResult.valid();
                }
                return uniquenessChecker.isVersionUnique(fieldToValidate.getText())
                        ? ValidationResult.valid()
                        : ValidationResult.invalid("This version number exists already.");
            }
        });
        formPanel.add(versionField);

        return formPanel;
    }

    private void buttonHandler(boolean okay) {
        if (okay && !formPanel.isFormValid()) {
            return;
        }
        wasOkayed = okay;
        dispose();
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createRaisedBevelBorder());

        JButton button = new JButton("OK");
        button.setPreferredSize(new Dimension(90, 24));
        button.addActionListener(e -> buttonHandler(true));
        panel.add(button);

        button = new JButton("Cancel");
        button.setPreferredSize(new Dimension(90, 24));
        button.addActionListener(e -> buttonHandler(false));
        panel.add(button);

        return panel;
    }

    public interface UniquenessChecker {
        boolean isVersionUnique(String version);
    }
}
