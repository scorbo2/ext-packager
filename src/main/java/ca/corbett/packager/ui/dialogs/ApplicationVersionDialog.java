package ca.corbett.packager.ui.dialogs;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ListField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.packager.ui.MainWindow;
import ca.corbett.updates.VersionManifest;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Shows a dialog for creating a new ApplicationVersion, or editing
 * an existing one.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ApplicationVersionDialog extends JDialog {

    private boolean wasOkayed;
    private FormPanel formPanel;
    private ShortTextField versionField;
    private UniquenessChecker uniquenessChecker;
    private final String applicationName;
    private ListField<VersionManifest.Extension> extensionListField;

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
        for (VersionManifest.Extension extension : appVersion.getExtensions()) {
            addExtension(extension);
        }
    }

    public VersionManifest.ApplicationVersion getApplicationVersion() {
        if (!wasOkayed) {
            return null;
        }

        VersionManifest.ApplicationVersion appVersion = new VersionManifest.ApplicationVersion();
        appVersion.setVersion(versionField.getText());

        DefaultListModel<VersionManifest.Extension> listModel = (DefaultListModel<VersionManifest.Extension>)extensionListField.getListModel();
        for (int i = 0; i < listModel.size(); i++) {
            appVersion.addExtension(listModel.elementAt(i));
        }

        // TODO populate extensions and extension versions and screenshots, oh my
        return appVersion;
    }

    private void addExtension(VersionManifest.Extension extension) {
        DefaultListModel<VersionManifest.Extension> listModel = (DefaultListModel<VersionManifest.Extension>)extensionListField.getListModel();
        listModel.addElement(extension);
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        formPanel.add(new LabelField("Application name:", applicationName));

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

        extensionListField = new ListField<>("Extensions:", List.of());
        extensionListField.setShouldExpand(true);
        extensionListField.setCellRenderer(new ExtensionListCellRenderer());
        formPanel.add(extensionListField);

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

    /**
     * A custom list cell renderer for displaying Extension instances in a user-friendly way.
     *
     * @author <a href="https://github.com/scorbo2">scorbo2</a>
     */
    private static class ExtensionListCellRenderer extends JLabel
            implements ListCellRenderer<VersionManifest.Extension> {

        @Override
        public Component getListCellRendererComponent(JList<? extends VersionManifest.Extension> list, VersionManifest.Extension value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getName() + " (" + value.getVersions().size() + " versions)");
            setOpaque(true);
            Color selectedFg = LookAndFeelManager.getLafColor("List.selectionForeground", Color.WHITE);
            Color selectedBg = LookAndFeelManager.getLafColor("List.selectionBackground", Color.BLUE);
            Color normalFg = LookAndFeelManager.getLafColor("List.foreground", Color.BLACK);
            Color normalBg = LookAndFeelManager.getLafColor("List.background", Color.WHITE);
            setForeground(isSelected ? selectedFg : normalFg);
            setBackground(isSelected ? selectedBg : normalBg);
            return this;
        }
    }

}
