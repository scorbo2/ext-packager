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
import ca.corbett.packager.ui.dialogs.NewProjectDialog;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This card allows creation or loading of an ext-packager project.
 * This card is singleton, and allows adding of ProjectListeners, so that
 * callers know when a new project is added. This card also maintains
 * a handle on the currently loaded Project, if any.
 * <pre>
 * // Get a handle on the current Project from anywhere:
 * Project project = ProjectCard.getInstance().getProject();
 * if (project != null) {
 *     // read it, update it, etc
 * }
 * </pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ProjectCard extends JPanel {

    private final Logger log = Logger.getLogger(ProjectCard.class.getName());
    private MessageUtil messageUtil;

    private static ProjectCard instance;
    private final List<ProjectListener> projectListeners = new ArrayList<>();
    private Project project = null;

    private final LabelField projectNameField;
    private final LabelField projectDirField;

    private ProjectCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Project selection", 20));

        projectNameField = new LabelField("Project name:", "N/A");
        formPanel.add(projectNameField);

        projectDirField = new LabelField("Project directory:", "N/A");
        formPanel.add(projectDirField);

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
    }

    public static ProjectCard getInstance() {
        if (instance == null) {
            instance = new ProjectCard();
        }
        return instance;
    }

    /**
     * Returns the currently loaded Project, or null if no selection has been made yet.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Registers to receive project notifications from this card. Most notably, you
     * can find out when a Project has been created/loaded. Other cards use this to
     * populate their fields based on whatever Project we're dealing with.
     */
    public void addProjectListener(ProjectListener listener) {
        projectListeners.add(listener);
    }

    public void removeProjectListener(ProjectListener listener) {
        projectListeners.remove(listener);
    }

    private void setProject(Project project) {
        projectNameField.setText(project.getName());
        projectDirField.setText(project.getProjectDir().getAbsolutePath());
        this.project = project;
        for (ProjectListener listener : projectListeners) {
            listener.projectLoaded(project);
        }
    }

    /**
     * Invoked internally to show a file chooser for opening an existing project from disk.
     */
    private void showBrowseProjectDialog() {
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
        if (fileChooser.showOpenDialog(MainWindow.getInstance()) == JFileChooser.APPROVE_OPTION) {
            try {
                File projectFile = fileChooser.getSelectedFile();
                Project project = Project.fromFile(projectFile);
                MainWindow.getInstance().projectOpened();
                setProject(project);

                // Remember this file browse location for next time:
                AppConfig.getInstance().setProjectBaseDir(projectFile.getParentFile().getParentFile());
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
                Project project = Project.createNew(dialog.getProjectName(), dialog.getProjectDir());
                MainWindow.getInstance().projectOpened();
                setProject(project);

                AppConfig.getInstance().setProjectBaseDir(dialog.getProjectDir().getParentFile());
                AppConfig.getInstance().save();

            }
            catch (IOException ioe) {
                getMessageUtil().error("Error creating project file: " + ioe.getMessage(), ioe);
            }
            MainWindow.getInstance().projectOpened();
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
