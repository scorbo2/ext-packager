package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.updates.UpdateSources;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.net.MalformedURLException;
import java.net.URL;

public class UpdateSourceDialog extends JDialog {

    private boolean wasOkayed;
    private FormPanel formPanel;
    private ShortTextField nameField;
    private ShortTextField manifestField;
    private ShortTextField publicKeyField;

    public UpdateSourceDialog(String title) {
        super(MainWindow.getInstance(), title, true);
        setSize(new Dimension(450, 220));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setLayout(new BorderLayout());
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    public boolean wasOkayed() {
        return wasOkayed;
    }

    public UpdateSources.UpdateSource getUpdateSource() {
        if (!wasOkayed) {
            return null;
        }

        String name = nameField.getText();
        URL manifestUrl = null;
        URL publicKeyUrl = null;
        try {
            manifestUrl = new URL(manifestField.getText());
            if (!publicKeyField.getText().isBlank()) {
                publicKeyUrl = new URL(publicKeyField.getText());
            }
        }
        catch (MalformedURLException ignored) {
            // it can't fail here because our validators guard us from getting this far if it isn't valid
        }
        return new UpdateSources.UpdateSource(name, manifestUrl, publicKeyUrl);
    }

    public void setUpdateSource(UpdateSources.UpdateSource updateSource) {
        nameField.setText(updateSource.getName());
        manifestField.setText(updateSource.getVersionManifestUrl().toString());
        if (updateSource.getPublicKeyUrl() == null) {
            publicKeyField.setText("");
        }
        else {
            publicKeyField.setText(updateSource.getPublicKeyUrl().toString());
        }
    }

    private void buttonHandler(boolean okay) {
        if (okay && !formPanel.isFormValid()) {
            return;
        }
        wasOkayed = okay;
        dispose();
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        nameField = new ShortTextField("Source name:", 20);
        nameField.setAllowBlank(false);
        formPanel.add(nameField);

        manifestField = new ShortTextField("Manifest URL:", 20);
        manifestField.addFieldValidator(new URLValidator(false));
        manifestField.setAllowBlank(false);
        formPanel.add(manifestField);

        publicKeyField = new ShortTextField("Public key URL:", 20);
        publicKeyField.addFieldValidator(new URLValidator(true));
        formPanel.add(publicKeyField);

        return formPanel;
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

    private static class URLValidator implements FieldValidator<ShortTextField> {

        private final boolean allowBlank;

        public URLValidator(boolean allowBlank) {
            this.allowBlank = allowBlank;
        }

        @Override
        public ValidationResult validate(ShortTextField fieldToValidate) {
            if (allowBlank && fieldToValidate.getText().isBlank()) {
                return ValidationResult.valid();
            }

            try {
                URL url = new URL(fieldToValidate.getText());
                String protocol = url.getProtocol().toLowerCase();
                if (protocol.equals("http") || protocol.equals("https") || protocol.equals("file")) {
                    return ValidationResult.valid();
                }
                return ValidationResult.invalid("Unsupported URL protocol: " + protocol);
            }
            catch (MalformedURLException e) {
                return ValidationResult.invalid("Invalid URL format.");
            }
        }
    }
}
