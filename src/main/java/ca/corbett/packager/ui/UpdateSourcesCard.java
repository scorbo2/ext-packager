package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class UpdateSourcesCard extends JPanel {

    public UpdateSourcesCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Update sources", 20));

        add(formPanel, BorderLayout.CENTER);
    }
}
