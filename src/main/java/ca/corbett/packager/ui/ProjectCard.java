package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class ProjectCard extends JPanel {

    public ProjectCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Project selection", 20));

        PanelField blah = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton btn = new JButton("blah");
        btn.addActionListener(e -> MainWindow.getInstance().projectOpened());
        blah.getPanel().add(btn);

        btn = new JButton("New Project");
        btn.addActionListener(e -> showNewProjectDialog());
        blah.getPanel().add(btn);
        formPanel.add(blah);

        add(formPanel, BorderLayout.CENTER);
    }

    private void showNewProjectDialog() {
        NewProjectDialog dialog = new NewProjectDialog();
        dialog.setVisible(true);
        if (dialog.wasOkayed()) {
            System.out.println(
                    "You created a new project named " + dialog.getProjectName() + " in directory " + dialog
                            .getProjectDir()
                            .getAbsolutePath());
        }
    }
}
