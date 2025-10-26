package ca.corbett.packager.ui;

import ca.corbett.extras.LookAndFeelManager;
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
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
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
import java.util.List;

public class UpdateSourcesCard extends JPanel implements ProjectListener {

    private final FormPanel formPanel;
    private final ShortTextField appNameField;
    private final ListField<UpdateSources.UpdateSource> sourcesListField;

    public UpdateSourcesCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Update sources", 20));

        appNameField = new ShortTextField("Application name:", 15);
        appNameField.addValueChangedListener(field -> generateSourcesJson());
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
        button.addActionListener(e -> addSource());
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

        ProjectCard.getInstance().addProjectListener(this);
    }

    private void addSource() {
        UpdateSourceDialog dialog = new UpdateSourceDialog("Add update source");
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }
        UpdateSources.UpdateSource updateSource = dialog.getUpdateSource();
        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        listModel.addElement(updateSource);
        generateSourcesJson();
    }

    private void editSource() {
        int[] selectedIndexes = sourcesListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        UpdateSourceDialog dialog = new UpdateSourceDialog("Edit update source");
        dialog.setUpdateSource(sourcesListField.getList().getSelectedValue());
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }

        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        listModel.insertElementAt(dialog.getUpdateSource(), selectedIndexes[0]); // insert new element
        listModel.removeElementAt(selectedIndexes[0] + 1); // remove old element which got bumped by one
        generateSourcesJson();
    }

    private void deleteSource() {
        int[] selectedIndexes = sourcesListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        listModel.removeElementAt(selectedIndexes[0]);
        generateSourcesJson();
    }

    private void generateSourcesJson() {
        if (!formPanel.isFormValid()) {
            return;
        }
        UpdateSources updateSources = new UpdateSources(appNameField.getText());
        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        for (int i = 0; i < listModel.size(); i++) {
            updateSources.addUpdateSource(listModel.getElementAt(i));
        }
        ProjectCard.getInstance().getProject().setUpdateSources(updateSources);
    }

    @Override
    public void projectLoaded(Project project) {
        // Dumb initial value in case the proper application name is not set:
        if (project == null || project.getUpdateSources() == null) {
            appNameField.setText("");
        }

        DefaultListModel<UpdateSources.UpdateSource> listModel = (DefaultListModel<UpdateSources.UpdateSource>)sourcesListField.getListModel();
        listModel.clear();

        if (project == null || project.getUpdateSources() == null) {
            return;
        }

        for (UpdateSources.UpdateSource updateSource : project.getUpdateSources().getUpdateSources()) {
            listModel.addElement(updateSource);
        }

        // Now we can set a more intelligent default value for application name:
        appNameField.setText(project.getUpdateSources().getApplicationName());
    }

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
}