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

public class ProjectCard extends JPanel {

    private final Logger log = Logger.getLogger(ProjectCard.class.getName());
    private MessageUtil messageUtil;

    private static ProjectCard instance;
    private final List<ProjectListener> projectListeners = new ArrayList<>();
    private Project project = null;

    private LabelField projectNameField;
    private LabelField projectDirField;

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

    public Project getProject() {
        return project;
    }

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

                AppConfig.getInstance().setProjectBaseDir(projectFile.getParentFile().getParentFile());
                AppConfig.getInstance().save();
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error reading project file: " + ioe.getMessage(), ioe);
            }
        }
    }

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
