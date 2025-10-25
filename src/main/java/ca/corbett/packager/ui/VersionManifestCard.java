package ca.corbett.packager.ui;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ListField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
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
import java.util.List;

public class VersionManifestCard extends JPanel implements ProjectListener {

    private final FormPanel formPanel;
    private final ShortTextField appNameField;
    private final ListField<VersionManifest.ApplicationVersion> appVersionListField;

    public VersionManifestCard() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Version manifest", 20));

        appNameField = new ShortTextField("Application name:", 15);
        appNameField.addValueChangedListener(field -> generateVersionManifestJson());
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
        button.addActionListener(e -> addApplicationVersion());
        button.setPreferredSize(new Dimension(90, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Edit");
        button.addActionListener(e -> editApplicationVersion());
        button.setPreferredSize(new Dimension(90, 24));
        buttonPanel.getPanel().add(button);

        button = new JButton("Delete");
        button.addActionListener(e -> deleteApplicationVersion());
        button.setPreferredSize(new Dimension(90, 24));
        buttonPanel.getPanel().add(button);
        buttonPanel.getMargins().setLeft(128);
        formPanel.add(buttonPanel);

        add(formPanel, BorderLayout.CENTER);
        ProjectCard.getInstance().addProjectListener(this);
    }

    private void addApplicationVersion() {
        ApplicationVersionDialog dialog = new ApplicationVersionDialog("Add application version");
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }
        VersionManifest.ApplicationVersion appVersion = dialog.getApplicationVersion();
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.addElement(appVersion);
        generateVersionManifestJson();
    }

    private void editApplicationVersion() {
        int[] selectedIndexes = appVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        ApplicationVersionDialog dialog = new ApplicationVersionDialog("Edit application version");
        dialog.setApplicationVersion(appVersionListField.getList().getSelectedValue());
        dialog.setVisible(true);
        if (!dialog.wasOkayed()) {
            return;
        }

        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.insertElementAt(dialog.getApplicationVersion(), selectedIndexes[0]); // insert new element
        listModel.removeElementAt(selectedIndexes[0] + 1); // remove old element which got bumped by one
        generateVersionManifestJson();
    }

    private void deleteApplicationVersion() {
        int[] selectedIndexes = appVersionListField.getSelectedIndexes();
        if (selectedIndexes.length == 0) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        DefaultListModel<VersionManifest.ApplicationVersion> listModel = (DefaultListModel<VersionManifest.ApplicationVersion>)appVersionListField.getListModel();
        listModel.removeElementAt(selectedIndexes[0]);
        generateVersionManifestJson();
    }

    private void generateVersionManifestJson() {
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
}
