package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.packager.Version;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainWindow extends JFrame {

    private static final Logger log = Logger.getLogger(MainWindow.class.getName());

    private static MainWindow instance;

    private MessageUtil messageUtil;
    private DefaultListModel<String> cardListModel;
    private JList<String> cardList;
    private JPanel contentPanel;
    private final Desktop desktop;

    private MainWindow() {
        super(Version.APPLICATION_NAME + " " + Version.VERSION);
        desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        setSize(new Dimension(700, 520));
        setMinimumSize(new Dimension(500, 350));
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenuPanel(), buildContentPanel());
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(195);
        add(splitPane, BorderLayout.CENTER);
        addContentPanel(new IntroCard(), "Overview");
        addContentPanel(new ProjectCard(), "Project");
        addContentPanel(new AboutCard(), "About");
        cardList.setSelectedIndex(0);
    }

    /**
     * Adds the remaining options to our menu now that a project is selected.
     */
    public void projectOpened() {
        addContentPanel(new KeyPairCard(), "Key management", 2);
        addContentPanel(new UpdateSourcesCard(), "Update sources", 3);
        addContentPanel(new VersionManifestCard(), "Version manifest", 4);
        addContentPanel(new JarSigningCard(), "Jar signing", 5);
        addContentPanel(new FTPUploadCard(), "FTP upload", 6);
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
        return instance;
    }

    private void addContentPanel(JPanel panel, String title) {
        addContentPanel(panel, title, -1);
    }

    private void addContentPanel(JPanel panel, String title, int index) {
        if (index == -1) {
            cardListModel.addElement("  " + title);
        }
        else {
            cardListModel.insertElementAt("  " + title, index);
        }
        contentPanel.add(PropertiesDialog.buildScrollPane(panel), title);
    }

    private JPanel buildMenuPanel() {
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        ImagePanel imagePanel = new ImagePanel(ImagePanelConfig.createSimpleReadOnlyProperties());
        BufferedImage image = null;
        try {
            URL url = getClass().getResource("/ca/corbett/extpackager/images/logo_wide.jpg");
            if (url != null) {
                image = ImageUtil.loadImage(url);
                image = ImageUtil.scaleImageToFitSquareBounds(image, 180);
            }
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to load logo image: " + ioe.getMessage(), ioe);
        }

        if (image != null) {
            imagePanel.setPreferredSize(new Dimension(180, 70));
            imagePanel.setImage(image);
            wrapperPanel.add(imagePanel, BorderLayout.NORTH);
        }

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BorderLayout());

        cardListModel = new DefaultListModel<>();
        cardList = new JList<>(cardListModel);
        cardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cardList.setFont(cardList.getFont().deriveFont(Font.PLAIN, 16f));
        listPanel.add(PropertiesDialog.buildScrollPane(cardList), BorderLayout.CENTER);

        cardList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || cardList.getSelectedValue() == null) {
                    return;
                }
                ((CardLayout)contentPanel.getLayout()).show(contentPanel, cardList.getSelectedValue().trim());
            }
        });

        wrapperPanel.add(listPanel, BorderLayout.CENTER);
        return wrapperPanel;
    }

    private JPanel buildContentPanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new CardLayout());
        return contentPanel;
    }

    public boolean isBrowsingSupported() {
        return desktop != null && desktop.isSupported(Desktop.Action.BROWSE);
    }

    public void openHyperlink(String link) {
        if (isBrowsingSupported()) {
            try {
                desktop.browse(new URL(link).toURI());
            }
            catch (Exception e) {
                log.warning("Unable to browse URI: " + e.getMessage());
            }
        }
        else {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(link), null);
            getMessageUtil().info(
                    "Hyperlinks are apparently not enabled in your JRE.\nLink copied to clipboard instead.");
        }
    }

    public static boolean isUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
