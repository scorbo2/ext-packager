package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.packager.AppConfig;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.dialogs.NewProjectDialog;
import ca.corbett.packager.ui.dialogs.PopupTextDialog;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This card allows creation or loading of an ext-packager project.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ProjectCard extends JPanel implements ProjectListener {

    private final Logger log = Logger.getLogger(ProjectCard.class.getName());
    private MessageUtil messageUtil;

    private final LabelField projectNameField;
    private final LabelField projectDirField;
    private final LabelField updateSourcesField;
    private final LabelField versionManifestField;

    public ProjectCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Project selection", 20));

        projectNameField = new LabelField("Project name:", "N/A");
        formPanel.add(projectNameField);

        projectDirField = new LabelField("Project directory:", "N/A");
        formPanel.add(projectDirField);

        updateSourcesField = new LabelField("Update sources:", "N/A");
        formPanel.add(updateSourcesField);

        versionManifestField = new LabelField("Version manifest", "N/A");
        formPanel.add(versionManifestField);

        PanelField buttonPanel = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton btn = new JButton("Open");
        btn.setPreferredSize(new Dimension(90, 24));
        btn.addActionListener(e -> showBrowseProjectDialog());
        buttonPanel.getPanel().add(btn);

        btn = new JButton("Create");
        btn.setPreferredSize(new Dimension(90, 24));
        btn.addActionListener(e -> showNewProjectDialog());
        buttonPanel.getPanel().add(btn);
        formPanel.add(buttonPanel);

        add(formPanel, BorderLayout.CENTER);

        ProjectManager.getInstance().addProjectListener(this);
    }

    /**
     * Invoked internally to show a file chooser for opening an existing project from disk.
     */
    private void showBrowseProjectDialog() {
        JFileChooser fileChooser = createProjectFileChooser();
        if (fileChooser.showOpenDialog(MainWindow.getInstance()) == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                ProjectManager.getInstance().loadProject(selectedFile);
                populateFields(ProjectManager.getInstance().getProject());

                // Remember this file browse location for next time:
                AppConfig.getInstance().setProjectBaseDir(selectedFile);
                AppConfig.getInstance().save();
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error reading project file: " + ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Invoked internally to show the dialog for creating a new ext-packager Project.
     */
    private void showNewProjectDialog() {
        NewProjectDialog dialog = new NewProjectDialog();
        dialog.setVisible(true);
        if (dialog.wasOkayed()) {
            try {
                ProjectManager.getInstance().newProject(dialog.getProjectName(), dialog.getProjectDir());
                populateFields(ProjectManager.getInstance().getProject());

                // Save this project base dir for next time:
                AppConfig.getInstance().setProjectBaseDir(dialog.getProjectDir().getParentFile());
                AppConfig.getInstance().save();
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error creating project file: " + ioe.getMessage(), ioe);
            }
            MainWindow.getInstance().projectOpened();
        }
    }

    private void populateFields(Project project) {
        projectNameField.setText(project.getName());
        projectDirField.setText(project.getProjectDir().getAbsolutePath());
        String filename = "update_sources.json";
        updateSourcesField.setText(filename);
        updateSourcesField.setHyperlink(createHyperlinkAction(filename,
                                                              ProjectManager.getInstance().getUpdateSourcesAsString()));

        filename = "dist/version_manifest.json";
        versionManifestField.setText(filename);
        versionManifestField.setHyperlink(createHyperlinkAction(filename,
                                                                ProjectManager.getInstance()
                                                                              .getVersionManifestAsString()));
    }

    private static AbstractAction createHyperlinkAction(String title, String contents) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PopupTextDialog(MainWindow.getInstance(), title, contents, false)
                        .setVisible(true);
            }
        };
    }

    /**
     * Creates and returns a JFileChooser suitable for choosing ext-packager project files.
     */
    private static JFileChooser createProjectFileChooser() {
        JFileChooser fileChooser = new JFileChooser(AppConfig.getInstance().getProjectBaseDir());
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".extpkg");
            }

            @Override
            public String getDescription() {
                return "ExtPackager project files (*.extpkg)";
            }
        });
        return fileChooser;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }

    @Override
    public void projectLoaded(Project project) {
        populateFields(project);
    }

    @Override
    public void projectSaved(Project project) {
        populateFields(project);
    }
}
