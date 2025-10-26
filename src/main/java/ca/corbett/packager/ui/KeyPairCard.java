package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.crypt.SignatureUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.dialogs.PopupTextDialog;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Logger;

/**
 * This card provides a way to generate a key pair for signing extension jars before they are
 * uploaded. Signing is optional but recommended. If this project already has a key pair,
 * you can regenerate it here, but be aware that this requires re-signing and re-uploading
 * ALL extension jars.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
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

        ProjectManager.getInstance().addProjectListener(this);
    }

    /**
     * Invoked internally to generate a key pair for use with this project.
     * If a key pair already exists, the user is prompted for confirmation to replace it.
     */
    private void generateKeyPair() {
        Project currentProject = ProjectManager.getInstance().getProject();
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

        ProjectManager.getInstance().removeProjectListener(this);
        try {
            KeyPair keyPair = SignatureUtil.generateKeyPair();
            ProjectManager.getInstance().getProject().setKeyPair(keyPair);
            ProjectManager.getInstance().save();
        }
        catch (Exception e) {
            getMessageUtil().error("Unable to generate key pair!: " + e.getMessage(), e);
        }
        finally {
            ProjectManager.getInstance().addProjectListener(this);
        }
    }

    private void populateFields(Project project) {
        PublicKey publicKey = project == null ? null : project.getPublicKey();
        if (publicKey != null) {
            publicKeyLabel.setText("dist/public.key");
            publicKeyLabel.setHyperlink(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new PopupTextDialog(MainWindow.getInstance(),
                                        "dist/public.key",
                                        ProjectManager.getInstance().getPublicKeyAsString(),
                                        false)
                            .setVisible(true);
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
                    new PopupTextDialog(MainWindow.getInstance(),
                                        "private.key",
                                        ProjectManager.getInstance().getPrivateKeyAsString(),
                                        false)
                            .setVisible(true);
                }
            });
        }
        else {
            privateKeyLabel.setText("N/A");
            privateKeyLabel.clearHyperlink();
        }
    }

    /**
     * We listen for changes to the currently selected Project - when a project is created
     * or opened, this method is invoked, and we use it to populate our public and private
     * key fields.
     */
    @Override
    public void projectLoaded(Project project) {
        populateFields(project);
    }

    @Override
    public void projectSaved(Project project) {
        populateFields(project);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
