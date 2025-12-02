package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.packager.Version;
import ca.corbett.packager.project.ProjectManager;

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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the main UI for this application, and provides access to all of the cards
 * that the user will interact with.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class MainWindow extends JFrame {

    private static final Logger log = Logger.getLogger(MainWindow.class.getName());

    private static MainWindow instance;

    private MessageUtil messageUtil;
    private DefaultListModel<String> cardListModel;
    private JList<String> cardList;
    private JPanel contentPanel;
    private final Desktop desktop;
    private File startupProjectFile = null;

    private MainWindow() {
        super(Version.APPLICATION_NAME + " " + Version.VERSION);
        desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        setSize(new Dimension(700, 520));
        setMinimumSize(new Dimension(500, 400));
        setLayout(new BorderLayout());
        setIconImage(loadIconResource("/ca/corbett/extpackager/images/logo.png", 64, 64));
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
        if (cardListModel.size() > 3) {
            return; // already done - only do it once
        }
        addContentPanel(new KeyPairCard(), "Key management", 2);
        addContentPanel(new UpdateSourcesCard(), "Update sources", 3);
        addContentPanel(new VersionManifestCard(), "Version manifest", 4);
        addContentPanel(new JarSigningCard(), "Jar signing", 5);
        addContentPanel(new UploadCard(), "Upload", 6);
    }

    /**
     * Invoked only from Main in the event we get a command line arg for a project to open on startup.
     * Calling this after MainWindow is shown does nothing.
     */
    public void setStartupProjectFile(File file) {
        startupProjectFile = file;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(true);
        if (startupProjectFile != null) {
            try {
                ProjectManager.getInstance().loadProject(startupProjectFile);
            }
            catch (IOException ioe) {
                getMessageUtil().error("Unable to load startup project: " + ioe.getMessage(), ioe);
            }
        }
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

    /**
     * Some JREs don't allow launching a hyperlink to open the browser.
     */
    public boolean isBrowsingSupported() {
        return desktop != null && desktop.isSupported(Desktop.Action.BROWSE);
    }

    /**
     * If hyperlink launching is supported, will open the user's default browser to the
     * given link (assuming the link is valid). If the JRE doesn't allow such things,
     * then the given link will be copied to the clipboard instead (better than nothing).
     */
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
            getMessageUtil().info("Hyperlinks are not enabled in your JRE.\nLink copied to clipboard instead.");
        }
    }

    /**
     * Loads and returns an image icon resource, scaling up or down to the given size if needed.
     *
     * @param resourceName The path to the resource file containing the image.
     * @param width        The desired width of the image.
     * @param height       The desired height of the image.
     * @return An image, loaded and scaled, or null if the resource was not found.
     */
    public static BufferedImage loadIconResource(String resourceName, int width, int height) {
        BufferedImage image = null;
        try {
            URL url = MainWindow.class.getResource(resourceName);
            if (url == null) {
                throw new IOException("Image resource not found: " + resourceName);
            }
            image = ImageUtil.loadImage(url);

            // If the width or height don't match, scale it up or down as needed:
            if (image.getWidth() != width || image.getHeight() != height) {
                image = ImageUtil.generateThumbnailWithTransparency(image, width, height);
            }
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Error loading image: " + ioe.getMessage(), ioe);
        }

        return image;
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
