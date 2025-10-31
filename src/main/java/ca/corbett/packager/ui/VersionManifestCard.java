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
import ca.corbett.packager.ui.dialogs.ExtensionVersionDialog;
import ca.corbett.updates.VersionManifest;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
    private final ListField<VersionManifest.Extension> extensionListField;
    private final ListField<VersionManifest.ExtensionVersion> extensionVersionListField;
    private boolean autoSave = false;

    public VersionManifestCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Version manifest", 20));

        appNameField = new ShortTextField("Application name:", 15);
        appNameField.addValueChangedListener(field -> saveChanges());
        appNameField.setAllowBlank(false);
        formPanel.add(appNameField);

        appVersionListField = buildAppVersionListField();
        formPanel.add(appVersionListField);
        formPanel.add(buildAppVersionButtonPanel());

        //noinspection unchecked
        extensionListField = (ListField<VersionManifest.Extension>)buildExtensionListField();
        formPanel.add(extensionListField);

        //noinspection unchecked
        extensionVersionListField = (ListField<VersionManifest.ExtensionVersion>)buildExtensionVersionListField();
        formPanel.add(extensionVersionListField);

        add(formPanel, BorderLayout.CENTER);
        ProjectManager.getInstance().addProjectListener(this);
        autoSave = true;
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
        }
    }

    private void addApplicationVersion(VersionManifest.ApplicationVersion version) {
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.addElement(version);
    }

    /**
     * Deletes the currently selected application version.
     */
    private void deleteApplicationVersion() {
        if (JOptionPane.showConfirmDialog(MainWindow.getInstance(),
                                          "Really delete selected application version?",
                                          "Confirm",
                                          JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }

        int[] selectedIndexes = appVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        VersionManifest.ApplicationVersion appVersion = listModel.getElementAt(selectedIndexes[0]);
        listModel.removeElementAt(selectedIndexes[0]);
        try {
            ProjectManager.getInstance().removeApplicationVersion(appVersion); // file cleanup
        }
        catch (IOException ioe) {
            getMessageUtil().error("Warning",
                                   "Not all files associated with this version could be removed: "
                                           + ioe.getMessage(), ioe);
        }
        populateFields(generateVersionManifest());
    }

    /**
     * Deletes the currently selected extension.
     */
    private void deleteExtension() {
        if (JOptionPane.showConfirmDialog(MainWindow.getInstance(),
                                          "Really delete selected extension?",
                                          "Confirm",
                                          JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }

        int[] selectedIndexes = extensionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<VersionManifest.Extension> listModel = (DefaultListModel<VersionManifest.Extension>)extensionListField.getListModel();
        VersionManifest.Extension extension = listModel.getElementAt(selectedIndexes[0]);
        listModel.removeElementAt(selectedIndexes[0]);
        try {
            ProjectManager.getInstance().removeExtension(extension); // file cleanup
        }
        catch (IOException ioe) {
            getMessageUtil().error("Warning",
                                   "Not all files associated with this extension could be removed: "
                                           + ioe.getMessage(), ioe);
        }
        populateFields(generateVersionManifest());
    }

    /**
     * Deletes the currently selected extension version.
     */
    private void deleteExtensionVersion() {
        if (JOptionPane.showConfirmDialog(MainWindow.getInstance(),
                                          "Really delete selected extension version?",
                                          "Confirm",
                                          JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }

        int[] selectedIndexes = extensionVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<VersionManifest.ExtensionVersion> listModel =
                (DefaultListModel<VersionManifest.ExtensionVersion>)extensionVersionListField.getListModel();
        VersionManifest.ExtensionVersion extensionVersion = listModel.getElementAt(selectedIndexes[0]);
        listModel.removeElementAt(selectedIndexes[0]);
        try {
            ProjectManager.getInstance().removeExtensionVersion(extensionVersion); // file cleanup
        }
        catch (IOException ioe) {
            getMessageUtil().error("Warning",
                                   "Not all files associated with this extension version could be removed: "
                                           + ioe.getMessage(), ioe);
        }
        populateFields(generateVersionManifest());
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
        if (!autoSave) {
            return;
        }
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
        autoSave = false; // wait until we're fully populated before saving

        try {
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
        finally {
            autoSave = true;
            saveChanges(); // save once after we're populated
        }
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
     * Will populate the subordinate list boxes based on whatever is selected in the app version list box.
     * Will clear the subordinate list boxes if no app version is selected.
     */
    private void appVersionSelectionChanged() {
        DefaultListModel<VersionManifest.ExtensionVersion> extensionVersionListModel =
                (DefaultListModel<VersionManifest.ExtensionVersion>)extensionVersionListField.getListModel();
        extensionVersionListModel.clear();

        DefaultListModel<VersionManifest.Extension> extensionListModel =
                (DefaultListModel<VersionManifest.Extension>)extensionListField.getListModel();
        extensionListModel.clear();

        int[] selectedIndexes = appVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            return;
        }

        DefaultListModel<VersionManifest.ApplicationVersion> appVersionListModel =
                (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        VersionManifest.ApplicationVersion appVersion = appVersionListModel.getElementAt(selectedIndexes[0]);
        for (VersionManifest.Extension extension : appVersion.getExtensions()) {
            extensionListModel.addElement(extension); // TODO sort maybe?
        }
    }

    private void extensionSelectionChanged() {
        DefaultListModel<VersionManifest.ExtensionVersion> extensionVersionListModel =
                (DefaultListModel<VersionManifest.ExtensionVersion>)extensionVersionListField.getListModel();
        extensionVersionListModel.clear();

        int[] selectedIndexes = extensionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            return;
        }

        DefaultListModel<VersionManifest.Extension> extensionListModel =
                (DefaultListModel<VersionManifest.Extension>)extensionListField.getListModel();
        VersionManifest.Extension extension = extensionListModel.getElementAt(selectedIndexes[0]);
        for (VersionManifest.ExtensionVersion extensionVersion : extension.getVersions()) {
            extensionVersionListModel.addElement(extensionVersion); // TODO sort maybe?
        }
    }

    private void editSelectedExtensionVersion() {
        // If no extension version is selected, we're done here:
        int[] selectedIndexes = extensionVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            return;
        }

        DefaultListModel<VersionManifest.ExtensionVersion> extensionVersionListModel =
                (DefaultListModel<VersionManifest.ExtensionVersion>)extensionVersionListField.getListModel();
        VersionManifest.ExtensionVersion extensionVersion = extensionVersionListModel.getElementAt(selectedIndexes[0]);
        ExtensionVersionDialog dialog = new ExtensionVersionDialog(extensionVersion);
        dialog.setVisible(true);
    }

    /**
     * Builds and returns a ListField for the given parameters.
     */
    private ListField<VersionManifest.ApplicationVersion> buildAppVersionListField() {
        ListField<VersionManifest.ApplicationVersion> listField;
        listField = new ListField<>("Versions:", List.of());
        listField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listField.setShouldExpand(true);
        listField.setCellRenderer(new AppVersionListCellRenderer());
        listField.addValueChangedListener(field -> appVersionSelectionChanged());

        addDeleteKeyboardHandler(listField, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteApplicationVersion();
            }
        });

        addDeletePopupMenu(listField, (ActionListener)e -> deleteApplicationVersion());

        return listField;
    }

    private PanelField buildAppVersionButtonPanel() {
        PanelField buttonPanel = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Import");
        button.addActionListener(e -> importExtensions());
        button.setPreferredSize(new Dimension(70, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Delete");
        button.addActionListener(e -> deleteApplicationVersion());
        button.setPreferredSize(new Dimension(70, 24));
        buttonPanel.getPanel().add(button);
        buttonPanel.getMargins().setLeft(128);

        return buttonPanel;
    }

    /**
     * Builds and returns the Extension list field.
     */
    private ListField<?> buildExtensionListField() {
        ListField<VersionManifest.Extension> listField;
        listField = new ListField<>("Extensions:", List.of());
        listField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listField.setShouldExpand(true);
        listField.setCellRenderer(new ExtensionListCellRenderer());
        listField.addValueChangedListener(field -> extensionSelectionChanged());

        addDeleteKeyboardHandler(listField, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteExtension();
            }
        });

        addDeletePopupMenu(listField, (ActionListener)e -> deleteExtension());

        return listField;
    }

    /**
     * Builds and returns the ExtensionVersion list field.
     */
    private ListField<?> buildExtensionVersionListField() {
        ListField<VersionManifest.ExtensionVersion> listField;
        listField = new ListField<>("Extension versions:", List.of());
        listField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listField.setShouldExpand(true);
        listField.setCellRenderer(new ExtensionVersionListCellRenderer());

        listField.getList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedExtensionVersion();
                }
            }
        });
        addDeleteKeyboardHandler(listField, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteExtensionVersion();
            }
        });

        addDeletePopupMenu(listField, (ActionListener)e -> deleteExtensionVersion());

        return listField;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }

    private static void addDeleteKeyboardHandler(ListField<?> listField, AbstractAction action) {
        // Allow delete key to delete selected item:
        InputMap inputMap = listField.getList().getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = listField.getList().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteAppVersion");
        actionMap.put("deleteAppVersion", action);
    }

    private static void addDeletePopupMenu(ListField<?> listField, ActionListener actionListener) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(actionListener);
        popupMenu.add(deleteItem);
        final JList<?> list = listField.getList();
        listField.getList().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            private void handlePopup(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                // Only show popup if click was on an actual item
                if (index != -1 && list.getCellBounds(index, index).contains(e.getPoint())) {
                    list.setSelectedIndex(index);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
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
     * A custom list cell renderer for displaying Extension instances in a user-friendly way.
     *
     * @author <a href="https://github.com/scorbo2">scorbo2</a>
     */
    private static class ExtensionListCellRenderer extends JLabel
            implements ListCellRenderer<VersionManifest.Extension> {

        @Override
        public Component getListCellRendererComponent(JList<? extends VersionManifest.Extension> list, VersionManifest.Extension value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getName() + " (" + value.getVersions().size() + " versions)");
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
     * A custom list cell renderer for display ExtensionVersion instances in a user-friendly way.
     */
    public static class ExtensionVersionListCellRenderer extends JLabel
            implements ListCellRenderer<VersionManifest.ExtensionVersion> {

        @Override
        public Component getListCellRendererComponent(JList<? extends VersionManifest.ExtensionVersion> list, VersionManifest.ExtensionVersion value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getExtInfo().getVersion() + " (" + value.getScreenshots().size() + " screenshots)");
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
}
