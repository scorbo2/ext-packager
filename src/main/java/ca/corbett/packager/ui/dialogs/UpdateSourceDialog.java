package ca.corbett.packager.ui.dialogs;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.packager.ui.MainWindow;
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

/**
 * Represents a dialog for creating a new UpdateSource, or editing an existing one.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class UpdateSourceDialog extends JDialog {

    private boolean wasOkayed;
    private FormPanel formPanel;
    private ShortTextField nameField;
    private ShortTextField baseUrlField;
    private LabelField manifestField;
    private LabelField publicKeyField;
    private LabelField extensionsDirField;

    public UpdateSourceDialog(String title) {
        super(MainWindow.getInstance(), title, true);
        setSize(new Dimension(600, 270));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setLayout(new BorderLayout());
        add(PropertiesDialog.buildScrollPane(buildFormPanel()), BorderLayout.CENTER);
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
        URL baseUrl = null;
        String manifest = "version_manifest.json"; // TODO maybe make this configurable?
        String publicKey = publicKeyField.getText().isBlank() ? null : "public.key"; // TODO configurable?
        try {
            baseUrl = new URL(baseUrlField.getText());
        }
        catch (MalformedURLException ignored) {
            // it can't fail here because our validators guard us from getting this far if it isn't valid
        }
        return new UpdateSources.UpdateSource(name, baseUrl, manifest, publicKey);
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
        String baseUrl = "";
        if (manifestField.getText().contains("/")) {
            String manifestUrl = manifestField.getText();
            baseUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf("/") + 1);
        }
        baseUrlField.setText(baseUrl);
        extensionsDirField.setText(baseUrl + "extensions/");
    }

    private void buttonHandler(boolean okay) {
        if (okay && !formPanel.isFormValid()) {
            return;
        }
        wasOkayed = okay;
        dispose();
    }

    /**
     * Invoked when text is typed in the base url field.
     * We create our version manifest url and public key url accordingly.
     */
    private void updateJsonUrls() {
        String baseUrl = baseUrlField.getText();
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        manifestField.setText(baseUrl + "version_manifest.json");
        publicKeyField.setText(baseUrl + "public.key");
        extensionsDirField.setText(baseUrl + "extensions/");
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        nameField = new ShortTextField("Source name:", 32);
        nameField.setAllowBlank(false);
        formPanel.add(nameField);

        baseUrlField = new ShortTextField("Base URL:", 32);
        baseUrlField.setAllowBlank(false);
        baseUrlField.addFieldValidator(new URLValidator(false));
        baseUrlField.addValueChangedListener(field -> updateJsonUrls());
        formPanel.add(baseUrlField);

        manifestField = new LabelField("Manifest URL:", "");
        formPanel.add(manifestField);

        publicKeyField = new LabelField("Public key URL:", "");
        formPanel.add(publicKeyField);

        extensionsDirField = new LabelField("Extensions:", "");
        formPanel.add(extensionsDirField);

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
