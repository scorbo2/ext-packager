package ca.corbett.packager.ui.dialogs;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.FileField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.packager.AppConfig;
import ca.corbett.packager.ui.MainWindow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.logging.Logger;

/**
 * Represents a dialog that has options for creating a new, empty ext-packager Project.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class NewProjectDialog extends JDialog {

    private static final Logger log = Logger.getLogger(NewProjectDialog.class.getName());
    private MessageUtil messageUtil;
    private boolean wasOkayed;
    private FormPanel formPanel;
    private ShortTextField nameField;
    private FileField baseDirField;
    private LabelField projectDirField;

    public NewProjectDialog() {
        super(MainWindow.getInstance(), "New Project", true);
        setSize(new Dimension(new Dimension(575, 210)));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setLayout(new BorderLayout());
        add(PropertiesDialog.buildScrollPane(buildFormPanel()), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        wasOkayed = false;
    }

    public boolean wasOkayed() {
        return wasOkayed;
    }

    public String getProjectName() {
        return nameField.getText();
    }

    public File getProjectDir() {
        return new File(baseDirField.getFile(), nameField.getText());
    }

    private void closeDialog(boolean wasOkayed) {
        if (wasOkayed && !formPanel.isFormValid()) {
            return; // directory doesn't exist or name was not valid
        }

        if (wasOkayed) {
            if (!baseDirField.getFile().isDirectory()
                    || !baseDirField.getFile().exists()
                    || !baseDirField.getFile().canWrite()) {
                getMessageUtil().error("Selected parent directory does not exist or is not accessible.");
                return;
            }

            File projectDir = new File(baseDirField.getFile(), nameField.getText());
            if (projectDir.exists() && !projectDir.isDirectory()) {
                getMessageUtil().error("A file named \""
                                               + nameField.getText()
                                               + "\" already exists in the parent dir.");
                return;
            }
            if (projectDir.exists() && projectDir.isDirectory() && projectDir.listFiles().length != 0) {
                if (JOptionPane.showConfirmDialog(this,
                                                  "The project directory already exists and is not empty!\n"
                                                          + "Really use this directory?",
                                                  "Confirm",
                                                  JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            if (!projectDir.exists() && !projectDir.mkdirs()) {
                getMessageUtil().error("Unable to create the project directory.");
                return;
            }

            AppConfig.getInstance().setProjectBaseDir(baseDirField.getFile());
            AppConfig.getInstance().save();
        }
        this.wasOkayed = wasOkayed;
        dispose();
    }

    private void updateProjectDir() {
        File baseDir = baseDirField.getFile();
        if (baseDir == null) {
            projectDirField.setText("");
            return;
        }
        projectDirField.setText(baseDir.getAbsolutePath() + File.separator + nameField.getText());
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        nameField = new ShortTextField("Project name:", 24);
        nameField.setText("New Project");
        nameField.setAllowBlank(false);
        nameField.addValueChangedListener(field -> updateProjectDir());
        formPanel.add(nameField);

        File projectBaseDir = AppConfig.getInstance().getProjectBaseDir();
        baseDirField = new FileField("Parent dir:",
                                     projectBaseDir,
                                     24,
                                     FileField.SelectionType.ExistingDirectory);
        baseDirField.addValueChangedListener(field -> updateProjectDir());
        formPanel.add(baseDirField);

        projectDirField = new LabelField("Project dir:",
                                         projectBaseDir.getAbsolutePath() + File.separator + "New Project");
        formPanel.add(projectDirField);

        return formPanel;
    }

    private JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton("OK");
        button.setPreferredSize(new Dimension(90, 25));
        button.addActionListener(e -> closeDialog(true));
        buttonPanel.add(button);

        button = new JButton("Cancel");
        button.setPreferredSize(new Dimension(90, 25));
        button.addActionListener(e -> closeDialog(false));
        buttonPanel.add(button);

        buttonPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        return buttonPanel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
