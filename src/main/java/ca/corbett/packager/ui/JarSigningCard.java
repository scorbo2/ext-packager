package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * This card provides a way to cryptographically sign all extension jars prior to upload,
 * and also to verify signatures that have previously been generated.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class JarSigningCard extends JPanel {

    public JarSigningCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Jar signing", 20));

        add(formPanel, BorderLayout.CENTER);
    }
}
