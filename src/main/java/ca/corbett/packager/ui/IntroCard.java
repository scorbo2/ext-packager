package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.packager.Version;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.net.URL;

public class IntroCard extends JPanel {

    public IntroCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel(Version.APPLICATION_NAME + " Overview", 20));

        LabelField label = new LabelField("Project page:", Version.PROJECT_URL);
        label.setHyperlink(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.getInstance().openHyperlink(Version.PROJECT_URL);
            }
        });
        formPanel.add(label);

        formPanel.add(LabelField.createPlainHeaderLabel("TODO overview goes here"));

        add(formPanel, BorderLayout.CENTER);
    }
}
