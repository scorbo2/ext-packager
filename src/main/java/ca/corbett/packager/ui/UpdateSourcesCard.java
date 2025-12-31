package ca.corbett.packager.ui;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ListField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.packager.io.FtpParams;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.dialogs.UpdateSourceDialog;
import ca.corbett.updates.UpdateSources;

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
 * This card allows viewing and editing of the update sources for the current project.
 * There are no limit to the number of update sources a project can have.
 * Each update source can either be web-based or filesystem based.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class UpdateSourcesCard extends JPanel implements ProjectListener {

    private static final Logger log = Logger.getLogger(UpdateSourcesCard.class.getName());
    private MessageUtil messageUtil;
    private final FormPanel formPanel;
    private final ShortTextField appNameField;
    private final ListField<UpdateSources.UpdateSource> sourcesListField;

    public UpdateSourcesCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Update sources", 20));

        appNameField = new ShortTextField("Application name:", 15);
        appNameField.addValueChangedListener(field -> saveChanges());
        appNameField.setAllowBlank(false);
        formPanel.add(appNameField);

        sourcesListField = new ListField<>("Update sources:", List.of());
        sourcesListField.addFieldValidator(new FieldValidator<FormField>() {
            @Override
            public ValidationResult validate(FormField fieldToValidate) {
                return sourcesListField.getListModel().getSize() == 0
                        ? ValidationResult.invalid("You must specify at least one source.")
                        : ValidationResult.valid();
            }
        });
        sourcesListField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sourcesListField.setShouldExpand(true);
        sourcesListField.setCellRenderer(new UpdateSourceRenderer());
        sourcesListField.getList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSource();
                }
            }
        });
        formPanel.add(sourcesListField);

        PanelField buttonPanel = new PanelField(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Add");
        button.addActionListener(e -> createSource());
        button.setPreferredSize(new Dimension(90, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Edit");
        button.addActionListener(e -> editSource());
        button.setPreferredSize(new Dimension(90, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Delete");
        button.addActionListener(e -> deleteSource());
        button.setPreferredSize(new Dimension(90, 24));
        buttonPanel.getPanel().add(button);
        buttonPanel.getMargins().setLeft(128);
        formPanel.add(buttonPanel);

        add(formPanel, BorderLayout.CENTER);

        ProjectManager.getInstance().addProjectListener(this);
    }

    /**
     * Shows a dialog which allows creating a new update source.
     */
    private void createSource() {
        UpdateSourceDialog dialog = new UpdateSourceDialog("Add update source");
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }
        UpdateSources.UpdateSource updateSource = dialog.getUpdateSource();
        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        listModel.addElement(updateSource);
        saveChanges();
    }

    /**
     * Shows a dialog which allows editing the details of an existing update source.
     * Requires an existing update source to be selected in the sources list.
     */
    private void editSource() {
        int[] selectedIndexes = sourcesListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }

        // Make a note of the name and whether it had a saved FTP params file:
        Project project = ProjectManager.getInstance().getProject();
        UpdateSources.UpdateSource updateSource = sourcesListField.getList().getSelectedValue();
        String oldName = updateSource.getName();
        FtpParams oldParams = null;
        if (FtpParams.ftpParamsExist(project, updateSource)) {
            try {
                FtpParams.fromUpdateSource(project, updateSource);
            }
            catch (IOException ioe) {
                // Just log the error, this isn't fatal:
                log.warning("Error loading existing FTP parameters for update source: " + ioe.getMessage());
            }
        }

        // Let the user edit the source:
        UpdateSourceDialog dialog = new UpdateSourceDialog("Edit update source");
        dialog.setUpdateSource(sourcesListField.getList().getSelectedValue());
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }

        // Insert the new one and then remove the old one:
        UpdateSources.UpdateSource newSource = dialog.getUpdateSource();
        sourcesListField.getListModel().insertElementAt(newSource, selectedIndexes[0]);
        sourcesListField.getListModel().removeElementAt(selectedIndexes[0] + 1);
        saveChanges();

        // If the user renamed the source, and there were FTP parameters saved for the old source,
        // we need to migrate those settings to the new name:
        if (!oldName.equals(newSource.getName()) && oldParams != null) {
            try {
                FtpParams.save(project, newSource, oldParams);
            }
            catch (IOException ioe) {
                // Just log the error, this isn't fatal:
                log.warning("Error saving migrated FTP parameters for update source: " + ioe.getMessage());
            }
        }
    }

    /**
     * Deletes the currently selected update source, assuming one is selected in the list.
     */
    private void deleteSource() {
        int[] selectedIndexes = sourcesListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        listModel.removeElementAt(selectedIndexes[0]);
        saveChanges();
    }

    /**
     * Invoked internally to commit changes to our update sources list.
     */
    private void saveChanges() {
        if (!formPanel.isFormValid()) {
            return;
        }
        UpdateSources updateSources = new UpdateSources(appNameField.getText());
        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        for (int i = 0; i < listModel.size(); i++) {
            updateSources.addUpdateSource(listModel.getElementAt(i));
        }
        ProjectManager.getInstance().getProject().setUpdateSources(updateSources);
        ProjectManager.getInstance().removeProjectListener(this);
        try {
            ProjectManager.getInstance().save();
        }
        catch (IOException ioe) {
            getMessageUtil().error("Error saving update sources: " + ioe.getMessage(), ioe);
        }
        finally {
            ProjectManager.getInstance().addProjectListener(this);
        }
    }

    private void populateFields(Project project) {
        // Blank out our current values:
        appNameField.setText("");
        sourcesListField.getListModel().clear();

        // If there's no project or no update sources, we're done:
        if (project == null || project.getUpdateSources() == null) {
            return;
        }

        // Populate our update sources list:
        for (UpdateSources.UpdateSource updateSource : project.getUpdateSources().getUpdateSources()) {
            sourcesListField.getListModel().addElement(updateSource);
        }

        // Populate our application name:
        appNameField.setText(project.getUpdateSources().getApplicationName());
    }

    /**
     * Fired just before a Project is loaded - we don't need to do anything here.
     */
    @Override
    public void projectWillLoad(Project ignored) {
        // No action needed
    }

    /**
     * Fired when a Project is loaded - we update our fields to reflect the current project's update sources.
     */
    @Override
    public void projectLoaded(Project project) {
        populateFields(project);
    }

    /**
     * Fired when a Project is saved - we update our fields to reflect any changes.
     *
     * @param project
     */
    @Override
    public void projectSaved(Project project) {
        populateFields(project);
    }

    /**
     * Fired when the current Project is closed - we blank out our fields.
     * @param project
     */
    @Override
    public void projectClosed(Project project) {
        populateFields(null);
    }

    /**
     * A custom ListRenderer that will display an UpdateSource in a user-friendly way.
     *
     * @author <a href="https://github.com/scorbo2">scorbo2</a>
     */
    private static class UpdateSourceRenderer extends JLabel implements ListCellRenderer<UpdateSources.UpdateSource> {

        @Override
        public Component getListCellRendererComponent(JList<? extends UpdateSources.UpdateSource> list, UpdateSources.UpdateSource value, int index, boolean isSelected, boolean cellHasFocus) {
            String baseUrl = "";
            String manifestUrl = value.getVersionManifestUrl().toString();
            if (manifestUrl.contains("/")) {
                baseUrl = ": " + manifestUrl.substring(0, manifestUrl.lastIndexOf("/") + 1);
            }
            setText(value.getName() + baseUrl);
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