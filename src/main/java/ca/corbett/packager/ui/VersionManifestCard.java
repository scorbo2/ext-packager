package ca.corbett.packager.ui;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ListField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.packager.AppConfig;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.dialogs.ApplicationVersionDialog;
import ca.corbett.updates.VersionManifest;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This card allows viewing and editing of the VersionManifest for the current Project.
 * This allows adding application versions, extensions, extension versions, and screenshots.
 * All of this ultimately gets packaged up into a version_manifest.json for uploading
 * to the remote UpdateSource.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class VersionManifestCard extends JPanel implements ProjectListener {

    private static final Logger log = Logger.getLogger(VersionManifestCard.class.getName());
    private MessageUtil messageUtil;
    private final FormPanel formPanel;
    private final ShortTextField appNameField;
    private final ListField<VersionManifest.ApplicationVersion> appVersionListField;

    public VersionManifestCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Version manifest", 20));

        appNameField = new ShortTextField("Application name:", 15);
        appNameField.addValueChangedListener(field -> saveChanges());
        appNameField.setAllowBlank(false);
        formPanel.add(appNameField);

        appVersionListField = new ListField<>("Versions:", List.of());
        appVersionListField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appVersionListField.setShouldExpand(true);
        appVersionListField.setCellRenderer(new AppVersionListCellRenderer());
        appVersionListField.getList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editApplicationVersion();
                }
            }
        });
        formPanel.add(appVersionListField);

        PanelField buttonPanel = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Add");
        button.addActionListener(e -> createApplicationVersion());
        button.setPreferredSize(new Dimension(70, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Edit");
        button.addActionListener(e -> editApplicationVersion());
        button.setPreferredSize(new Dimension(70, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Delete");
        button.addActionListener(e -> deleteApplicationVersion());
        button.setPreferredSize(new Dimension(70, 24));
        buttonPanel.getPanel().add(button);
        buttonPanel.getMargins().setLeft(128);

        button = new JButton("Import");
        button.addActionListener(e -> importExtensions());
        button.setPreferredSize(new Dimension(70, 24));
        buttonPanel.getPanel().add(button);
        buttonPanel.getMargins().setLeft(128);
        formPanel.add(buttonPanel);

        add(formPanel, BorderLayout.CENTER);
        ProjectManager.getInstance().addProjectListener(this);
    }

    /**
     * Pops a dialog for manually creating a new application version.
     */
    private void createApplicationVersion() {
        ApplicationVersionDialog dialog = new ApplicationVersionDialog("Add application version",
                                                                       appNameField.getText());
        dialog.setUniquenessChecker(new ApplicationVersionDialog.UniquenessChecker() {
            @Override
            public boolean isVersionUnique(String version) {
                DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.getElementAt(i).getVersion().equals(version)) {
                        return false;
                    }
                }
                return true;
            }
        });
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }

        addApplicationVersion(dialog.getApplicationVersion());
    }

    /**
     * Pops a file browser for automatically importing extension jars.
     */
    private void importExtensions() {
        JFileChooser fileChooser = createExtensionFileChooser();
        if (fileChooser.showOpenDialog(MainWindow.getInstance()) == JFileChooser.CANCEL_OPTION) {
            return;
        }

        Set<File> extensionJars = new HashSet<>();
        for (File file : fileChooser.getSelectedFiles()) {
            if (file.isFile()) {
                extensionJars.add(file);
            }
            else if (file.isDirectory()) {
                extensionJars.addAll(FileSystemUtil.findFiles(file, true, "jar"));
            }
        }
        int succeeded = 0;
        VersionManifest versionManifest = generateVersionManifest();
        for (File candidateJar : extensionJars) {
            try {
                ProjectManager.getInstance().importExtensionJar(versionManifest, candidateJar);
                succeeded++;
            }
            catch (Exception e) {
                log.warning(e.getMessage());
                log.log(Level.FINE, "Problem with jar " + candidateJar.getAbsolutePath() + ": " + e.getMessage(), e);
            }
        }

        if (succeeded != extensionJars.size()) {
            getMessageUtil().warning("Warning", "Not all jars could be imported. See log for details.");
        }
        if (succeeded > 0) {
            getMessageUtil().info("Successfully imported " + succeeded + " extension jars.");
            populateFields(versionManifest);
            saveChanges();
        }
    }

    private void addApplicationVersion(VersionManifest.ApplicationVersion version) {
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.addElement(version);
        saveChanges();
    }

    /**
     * Allows editing the currently selected application version.
     */
    private void editApplicationVersion() {
        int[] selectedIndexes = appVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        ApplicationVersionDialog dialog = new ApplicationVersionDialog("Edit application version",
                                                                       appNameField.getText());
        dialog.setApplicationVersion(appVersionListField.getList().getSelectedValue());
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }

        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.insertElementAt(dialog.getApplicationVersion(), selectedIndexes[0]); // insert new element
        listModel.removeElementAt(selectedIndexes[0] + 1); // remove old element which got bumped by one
        saveChanges();
    }

    /**
     * Deletes the currently selected application version.
     */
    private void deleteApplicationVersion() {
        int[] selectedIndexes = appVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.removeElementAt(selectedIndexes[0]);
        saveChanges();
    }

    /**
     * Given the current state of our UI fields, generate and return a VersionManifest.
     */
    private VersionManifest generateVersionManifest() {
        VersionManifest manifest = new VersionManifest();
        manifest.setApplicationName(appNameField.getText());
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        for (int i = 0; i < listModel.size(); i++) {
            manifest.addApplicationVersion(listModel.getElementAt(i));
        }
        //manifest.setManifestGenerated(); // TODO do we set this each time? or once on upload?
        return manifest;
    }

    /**
     * Invoked internally to commit changes to the version manifest.
     */
    private void saveChanges() {
        if (!formPanel.isFormValid()) {
            return;
        }

        ProjectManager.getInstance().getProject().setVersionManifest(generateVersionManifest());
        ProjectManager.getInstance().removeProjectListener(this);
        try {
            ProjectManager.getInstance().save();
        }
        catch (IOException ioe) {
            getMessageUtil().error("Error saving version manifest: " + ioe.getMessage(), ioe);
        }
        finally {
            ProjectManager.getInstance().addProjectListener(this);
        }
    }

    private void populateFields(VersionManifest versionManifest) {
        // Dumb initial value in case the proper application name is not set:
        if (versionManifest == null) {
            appNameField.setText("");
            return;
        }

        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.clear();
        List<VersionManifest.ApplicationVersion> sortedList = versionManifest
                .getApplicationVersions()
                .stream()
                .sorted((a, b) -> a.getVersion().compareTo(b.getVersion()))
                .toList();
        for (VersionManifest.ApplicationVersion version : sortedList) {
            addApplicationVersion(version);
        }

        // Now we can set a more intelligent default value for application name:
        appNameField.setText(versionManifest.getApplicationName());
    }

    /**
     * We listen for project events so that when a project is loaded, we can parse out the
     * version manifest from it and display it here.
     */
    @Override
    public void projectLoaded(Project project) {
        populateFields(project.getVersionManifest());
    }

    @Override
    public void projectSaved(Project project) {
        populateFields(project.getVersionManifest());
    }

    /**
     * A custom list cell renderer for displaying ApplicationVersion instances in a user-friendly way.
     *
     * @author <a href="https://github.com/scorbo2">scorbo2</a>
     */
    private static class AppVersionListCellRenderer extends JLabel
            implements ListCellRenderer<VersionManifest.ApplicationVersion> {

        @Override
        public Component getListCellRendererComponent(JList<? extends VersionManifest.ApplicationVersion> list, VersionManifest.ApplicationVersion value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getVersion() + " (" + value.getExtensions().size() + " extensions)");
            setOpaque(true);
            Color selectedFg = LookAndFeelManager.getLafColor("List.selectionForeground", Color.WHITE);
            Color selectedBg = LookAndFeelManager.getLafColor("List.selectionBackground", Color.BLUE);
            Color normalFg = LookAndFeelManager.getLafColor("List.foreground", Color.BLACK);
            Color normalBg = LookAndFeelManager.getLafColor("List.background", Color.WHITE);
            setForeground(isSelected ? selectedFg : normalFg);
            setBackground(isSelected ? selectedBg : normalBg);
            return this;
        }
    }

    /**
     * Creates and returns a JFileChooser suitable for choosing jars or directories of jars.
     */
    private static JFileChooser createExtensionFileChooser() {
        JFileChooser fileChooser = new JFileChooser(AppConfig.getInstance().getProjectBaseDir());
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".jar");
            }

            @Override
            public String getDescription() {
                return "Jar files (*.jar)";
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
}
