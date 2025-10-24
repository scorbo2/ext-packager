package ca.corbett.packager.ui;

import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.updates.VersionManifest;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class VersionManifestCard extends JPanel implements ProjectListener {

    private final FormPanel formPanel;
    private final ShortTextField appNameField;

    public VersionManifestCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Version manifest", 20));

        appNameField = new ShortTextField("Application name:", 15);
        appNameField.addValueChangedListener(field -> generateVersionManifestJson());
        appNameField.setAllowBlank(false);
        formPanel.add(appNameField);


        add(formPanel, BorderLayout.CENTER);
        ProjectCard.getInstance().addProjectListener(this);
    }

    private void generateVersionManifestJson() {
        if (!formPanel.isFormValid()) {
            return;
        }

        VersionManifest manifest = new VersionManifest();
        manifest.setApplicationName(appNameField.getText());
        //manifest.setManifestGenerated(); // TODO do we set this each time? or once on upload?

        // TODO extensions, ext versions, screenshots

        ProjectCard.getInstance().getProject().setVersionManifest(manifest);
    }

    @Override
    public void projectLoaded(Project project) {
        // Dumb initial value in case the proper application name is not set:
        if (project == null || project.getUpdateSources() == null) {
            appNameField.setText("");
        }

        if (project == null || project.getUpdateSources() == null) {
            return;
        }

        // TODO load version manifest settings from project

        // Now we can set a more intelligent default value for application name:
        appNameField.setText(project.getUpdateSources().getApplicationName());
    }
}
