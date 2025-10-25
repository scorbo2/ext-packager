package ca.corbett.packager.ui;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ShortTextField;
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
    private ShortTextField versionField;

    public ApplicationVersionDialog(String title) {
        super(MainWindow.getInstance(), title, true);
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

    public void setApplicationVersion(VersionManifest.ApplicationVersion appVersion) {
        versionField.setText(appVersion.getVersion());
    }

    public VersionManifest.ApplicationVersion getApplicationVersion() {
        if (!wasOkayed) {
            return null;
        }

        VersionManifest.ApplicationVersion appVersion = new VersionManifest.ApplicationVersion();
        appVersion.setVersion(versionField.getText()); // TODO uniqueness check...
        // TODO populate extensions and extension versions and screenshots, oh my
        return appVersion;
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        versionField = new ShortTextField("Application version:", 10);
        versionField.setAllowBlank(false);
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
}
