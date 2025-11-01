package ca.corbett.packager.ui.dialogs;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ImageListField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.ui.MainWindow;
import ca.corbett.updates.VersionManifest;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This dialog shows details for a specific ExtensionVersion: download path, signature path,
 * extInfo details, and screenshots.
 * <p>
 * Can add or remove screenshots here, but all other details are read-only on this dialog.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ExtensionVersionDialog extends JDialog {

    private static final Logger log = Logger.getLogger(ExtensionVersionDialog.class.getName());
    private MessageUtil messageUtil;
    private boolean wasOkayed;
    private final VersionManifest.ExtensionVersion extensionVersion;
    private boolean screenshotsModified = false;
    private ImageListField screenshotsField;

    public ExtensionVersionDialog(VersionManifest.ExtensionVersion version) {
        super(MainWindow.getInstance(), "Extension version: "
                +version.getExtInfo().getName()
                + " "
                + version.getExtInfo().getVersion(), true);
        this.extensionVersion = version;
        setSize(new Dimension(new Dimension(575, 510)));
        setResizable(true);
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

    private FormPanel buildFormPanel() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);

        formPanel.add(new LabelField("Name:", extensionVersion.getExtInfo().getName()));
        formPanel.add(new LabelField("Version:", extensionVersion.getExtInfo().getVersion()));
        formPanel.add(new LabelField("Description:", extensionVersion.getExtInfo().getShortDescription()));
        addHyperlinkField(formPanel, "Details:", extensionVersion.getExtInfo().getLongDescription());
        addHyperlinkField(formPanel, "Project URL:", extensionVersion.getExtInfo().getProjectUrl());
        formPanel.add(new LabelField("Target:", extensionVersion.getExtInfo().getTargetAppName()
                + " "
                + extensionVersion.getExtInfo().getTargetAppVersion()));
        String author = extensionVersion.getExtInfo().getAuthor();
        String authorUrl = extensionVersion.getExtInfo().getAuthorUrl();
        if (author != null && !author.isBlank()) {
            LabelField labelField = new LabelField("Author:", author);
            if (authorUrl != null && !authorUrl.isBlank()) {
                labelField.setHyperlink(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        MainWindow.getInstance().openHyperlink(authorUrl);
                    }
                });
            }
            formPanel.add(labelField);
        }
        addHyperlinkField(formPanel, "Release notes:", extensionVersion.getExtInfo().getReleaseNotes());

        formPanel.add(new LabelField("Download jar:", extensionVersion.getDownloadPath()));
        String signaturePath = extensionVersion.getSignaturePath();
        if (signaturePath != null && !signaturePath.isBlank()) {
            File signatureFile = ProjectManager.getInstance().getProjectFileFromPath(extensionVersion, signaturePath);
            LabelField labelField = new LabelField("Signature:", signaturePath);
            try {
                if (signatureFile == null) {
                    throw new IOException("Signature file not found.");
                }
                final PopupTextDialog dialog = new PopupTextDialog(MainWindow.getInstance(),
                                                                   "Signature:",
                                                                   FileSystemUtil.readFileToString(signatureFile),
                                                                   false);
                labelField.setHyperlink(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.setVisible(true);
                    }
                });
            }
            catch (IOException ioe) {
                getMessageUtil().error("Unable to read signature file: "+ioe.getMessage(), ioe);
            }
            formPanel.add(labelField);
        }

        screenshotsField = new ImageListField("Screenshots:", 1);
        screenshotsField.setShouldExpand(true);
        //screenshotsField.getImageListPanel().setOwnerFrame(this); // TODO Needs swing-extras #159
        for (String screenshotPath : extensionVersion.getScreenshots()) {
            try {
                File screenshotFile = ProjectManager.getInstance().getProjectFileFromPath(extensionVersion, screenshotPath);
                if (screenshotFile == null) {
                    throw new IOException("Screenshot file not found.");
                }
                screenshotsField.addImage(ImageUtil.loadImage(screenshotFile));
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error loading screenshot "+screenshotPath+": "+ioe.getMessage(), ioe);
            }
        }
        screenshotsField.addValueChangedListener(field -> screenshotsModified = true);
        formPanel.add(screenshotsField);

    	return formPanel;
    }

    private void addHyperlinkField(FormPanel formPanel, String label, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        LabelField labelField = new LabelField(label, "click to view");
        labelField.setHyperlink(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PopupTextDialog(MainWindow.getInstance(), label, text, false).setVisible(true);
            }
        });
        formPanel.add(labelField);
    }

    private void closeDialog(boolean okay) {
        // There's nothing to validate here because the form is basically read-only except for screenshots,
        // and there's no validation required for screenshots. But if we DID need to validate...
//    	if (okay && ! formPanel.isFormValid()) {
//    		return;
//    	}

        // If any screenshot was added or removed, nuke the entire existing list and rebuild it
        // from our ImageListField. This is not terribly surgical but there's currently no way of knowing
        // which images map to which existing file(s) after a modification is made, so...
        if (screenshotsModified) {
            try {
                ProjectManager.getInstance().removeAllScreenshots(extensionVersion);
                extensionVersion.clearScreenshots();
                for (int i = 0; i < screenshotsField.getImageCount(); i++) {
                    String jarPath = extensionVersion.getDownloadPath();
                    if (jarPath.toLowerCase().endsWith(".jar")) {
                        jarPath = jarPath.substring(0, jarPath.length() - 4);
                    }
                    File screenshotFile = ProjectManager.getInstance()
                                                        .getProjectFileFromPath(
                                                                extensionVersion,
                                                                jarPath + "_screenshot" + (i + 1) + ".jpg");
                    ImageUtil.saveImage(screenshotsField.getImageListPanel().getImageAt(i), screenshotFile);
                    extensionVersion.addScreenshot(screenshotFile.getName());
                }
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error updating screenshots: " + ioe.getMessage(), ioe);
            }
        }

    	this.wasOkayed = okay;
    	dispose();
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
