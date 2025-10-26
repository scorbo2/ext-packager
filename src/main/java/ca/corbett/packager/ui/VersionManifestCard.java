package ca.corbett.packager.ui;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ListField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.dialogs.ApplicationVersionDialog;
import ca.corbett.updates.VersionManifest;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
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
        // TODO pop a file/directory chooser
        // If a directory is chosen, scan it recursively for all jar files
        // otherwise, allow individual jar file(s) to be chosen
        // either way, extract the extInfo.json out of each one
        // if its targetApplicationVersion already exists, add this extension to that version
        // otherwise, implicitly create an ApplicationVersion, add it to the list, and add this extension to it
        // Sort the ApplicationVersion list after we're done?
        //
        // NOTE: the extension(s) we find may already exist!
        //       Overwrite if different from what we have, or leave it alone if it matches
        //       This allows a batch import to safely add any new or changed extensions without duplicating existing ones
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
     * Invoked internally to commit changes to the version manifest.
     */
    private void saveChanges() {
        if (!formPanel.isFormValid()) {
            return;
        }

        VersionManifest manifest = new VersionManifest();
        manifest.setApplicationName(appNameField.getText());
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        for (int i = 0; i < listModel.size(); i++) {
            manifest.addApplicationVersion(listModel.getElementAt(i));
        }
        //manifest.setManifestGenerated(); // TODO do we set this each time? or once on upload?

        ProjectManager.getInstance().getProject().setVersionManifest(manifest);
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

    private void populateFields(Project project) {
        // Dumb initial value in case the proper application name is not set:
        if (project == null || project.getVersionManifest() == null) {
            appNameField.setText("");
        }

        if (project == null || project.getVersionManifest() == null) {
            return;
        }

        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.clear();
        for (VersionManifest.ApplicationVersion version : project.getVersionManifest().getApplicationVersions()) {
            addApplicationVersion(version);
        }

        // Now we can set a more intelligent default value for application name:
        appNameField.setText(project.getVersionManifest().getApplicationName());
    }

    /**
     * We listen for project events so that when a project is loaded, we can parse out the
     * version manifest from it and display it here.
     */
    @Override
    public void projectLoaded(Project project) {
        populateFields(project);
    }

    @Override
    public void projectSaved(Project project) {
        populateFields(project);
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

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
