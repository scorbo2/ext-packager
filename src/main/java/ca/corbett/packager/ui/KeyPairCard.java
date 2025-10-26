package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.crypt.SignatureUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.ui.dialogs.PopupTextDialog;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Logger;

public class KeyPairCard extends JPanel implements ProjectListener {

    private static final Logger log = Logger.getLogger(KeyPairCard.class.getName());
    private MessageUtil messageUtil;

    private final LabelField privateKeyLabel;
    private final LabelField publicKeyLabel;

    public KeyPairCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Key management", 20));

        privateKeyLabel = new LabelField("Private key:", "N/A");
        formPanel.add(privateKeyLabel);

        publicKeyLabel = new LabelField("Public key:", "N/A");
        formPanel.add(publicKeyLabel);

        PanelField panelField = new PanelField(new FlowLayout());
        JButton generateKeyButton = new JButton("Generate new");
        generateKeyButton.setPreferredSize(new Dimension(120, 25));
        generateKeyButton.addActionListener(e -> generateKeyPair());
        panelField.getPanel().add(generateKeyButton);
        formPanel.add(panelField);

        add(formPanel, BorderLayout.CENTER);

        ProjectCard.getInstance().addProjectListener(this);
    }

    private void generateKeyPair() {
        Project currentProject = ProjectCard.getInstance().getProject();
        if (currentProject.getPublicKey() != null || currentProject.getPrivateKey() != null) {
            if (JOptionPane.showConfirmDialog(MainWindow.getInstance(),
                                              "WARNING!\nThis project already has a key pair defined.\n"
                                                      + "If you generate a new key pair, you must re-sign and\n"
                                                      + "re-upload ALL extension jars!\n\n"
                                                      + "Really regenerate key pair?",
                                              "Confirm key pair regeneration",
                                              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        try {
            KeyPair keyPair = SignatureUtil.generateKeyPair();
            ProjectCard.getInstance().getProject().setPublicKey(keyPair.getPublic());
            ProjectCard.getInstance().getProject().setPrivateKey(keyPair.getPrivate());
            projectLoaded(ProjectCard.getInstance().getProject());
        }
        catch (Exception e) {
            getMessageUtil().error("Unable to generate key pair!: " + e.getMessage(), e);
        }
    }

    private void showTextFromFile(String label, File file) {
        try {
            PopupTextDialog dialog = new PopupTextDialog(MainWindow.getInstance(),
                                                         label,
                                                         FileSystemUtil.readFileToString(file),
                                                         false);
            dialog.setSize(new Dimension(600, 400));
            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
        }
        catch (IOException ioe) {
            getMessageUtil().error("Unable to read key file: " + ioe.getMessage(), ioe);
        }
    }

    @Override
    public void projectLoaded(Project project) {
        PublicKey publicKey = project == null ? null : project.getPublicKey();
        if (publicKey != null) {
            publicKeyLabel.setText("public.key");
            publicKeyLabel.setHyperlink(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showTextFromFile("Public key:",
                                     new File(ProjectCard.getInstance().getProject().getDistDir(), "public.key"));
                }
            });
        }
        else {
            publicKeyLabel.setText("N/A");
            publicKeyLabel.clearHyperlink();
        }

        PrivateKey privateKey = project == null ? null : project.getPrivateKey();
        if (privateKey != null) {
            privateKeyLabel.setText("private.key");
            privateKeyLabel.setHyperlink(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showTextFromFile("Private key:",
                                     new File(ProjectCard.getInstance().getProject().getProjectDir(), "private.key"));
                }
            });
        }
        else {
            privateKeyLabel.setText("N/A");
            privateKeyLabel.clearHyperlink();
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
