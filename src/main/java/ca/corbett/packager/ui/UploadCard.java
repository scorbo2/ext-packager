package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Provides a means to upload all files to a given UpdateSource, either by ftp or
 * by local filesystem copies, depending on the source.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class UploadCard extends JPanel {

    public UploadCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Upload", 20));

        add(formPanel, BorderLayout.CENTER);
    }
}
