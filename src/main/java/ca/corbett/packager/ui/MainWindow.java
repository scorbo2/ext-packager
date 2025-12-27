package ca.corbett.packager.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.packager.Version;
import ca.corbett.packager.project.Project;
import ca.corbett.packager.project.ProjectListener;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the main UI for this application, and provides access to all of the cards
 * that the user will interact with.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class MainWindow extends JFrame implements ProjectListener {

    private static final Logger log = Logger.getLogger(MainWindow.class.getName());

    private static MainWindow instance;

    private static final String CARD_INTRO = "Overview";
    private static final String CARD_PROJECT = "Project";
    private static final String CARD_ABOUT = "About";
    private static final String CARD_KEYPAIR = "Key management";
    private static final String CARD_UPDATE_SOURCES = "Update sources";
    private static final String CARD_VERSION_MANIFEST = "Version manifest";
    private static final String CARD_JAR_SIGNING = "Jar signing";
    private static final String CARD_UPLOAD = "Upload";

    private MessageUtil messageUtil;
    private DefaultListModel<String> cardListModel;
    private JList<String> cardList;
    private JPanel contentPanel;
    private File startupProjectFile = null;
    private LinkedHashMap<String, JPanel> cardMap = new LinkedHashMap<>();

    private MainWindow() {
        super(Version.APPLICATION_NAME + " " + Version.VERSION);
        setSize(new Dimension(700, 520));
        setMinimumSize(new Dimension(500, 400));
        setLayout(new BorderLayout());
        setIconImage(loadIconResource("/ca/corbett/extpackager/images/logo.png", 64, 64));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenuPanel(), buildContentPanel());
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(195);
        add(splitPane, BorderLayout.CENTER);

        cardMap = new LinkedHashMap<>();
        cardMap.put(CARD_INTRO, new IntroCard());
        cardMap.put(CARD_PROJECT, new ProjectCard());
        cardMap.put(CARD_KEYPAIR, new KeyPairCard());
        cardMap.put(CARD_UPDATE_SOURCES, new UpdateSourcesCard());
        cardMap.put(CARD_VERSION_MANIFEST, new VersionManifestCard());
        cardMap.put(CARD_JAR_SIGNING, new JarSigningCard());
        cardMap.put(CARD_UPLOAD, new UploadCard());
        cardMap.put(CARD_ABOUT, new AboutCard());

        addContentPanel(cardMap.get(CARD_INTRO), CARD_INTRO);
        addContentPanel(cardMap.get(CARD_PROJECT), CARD_PROJECT);
        addContentPanel(cardMap.get(CARD_ABOUT), CARD_ABOUT);
        cardList.setSelectedIndex(0);
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
        ProjectManager.getInstance().addProjectListener(this);
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

    /**
     * Invoked internally to add the given panel as a new card in the content area,
     * using the given title as the menu link in our list on the left.
     */
    private void addContentPanel(JPanel panel, String title) {
        addContentPanel(panel, title, -1);
    }

    /**
     * Invoked internally to add the given panel as a new card in the content area,
     * using the given title as the menu link in our list on the left, at the
     * specified index (or at the end if index is -1).
     */
    private void addContentPanel(JPanel panel, String title, int index) {
        if (index == -1) {
            cardListModel.addElement("  " + title);
        }
        else {
            cardListModel.insertElementAt("  " + title, index);
        }
        contentPanel.add(PropertiesDialog.buildScrollPane(panel), title);
    }

    /**
     * Invoked internally to remove the given item from our menu list and content area.
     */
    private void removeContentPanel(String title) {
        int index = cardListModel.indexOf("  " + title);
        if (index != -1) {
            cardListModel.remove(index);
            contentPanel.remove(contentPanel.getComponent(index));
        }
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

    /**
     * Invoked from ProjectManager when a project is about to be loaded.
     * We respond to this by adding the remaining cards to our UI, if they
     * have not already been added.
     */
    @Override
    public void projectWillLoad(Project project) {
        if (cardListModel.size() > 3) {
            return; // already done - only do it once
        }
        addContentPanel(cardMap.get(CARD_KEYPAIR), CARD_KEYPAIR, 2);
        addContentPanel(cardMap.get(CARD_UPDATE_SOURCES), CARD_UPDATE_SOURCES, 3);
        addContentPanel(cardMap.get(CARD_VERSION_MANIFEST), CARD_VERSION_MANIFEST, 4);
        addContentPanel(cardMap.get(CARD_JAR_SIGNING), CARD_JAR_SIGNING, 5);
        addContentPanel(cardMap.get(CARD_UPLOAD), CARD_UPLOAD, 6);
    }

    @Override
    public void projectLoaded(Project project) {

    }

    @Override
    public void projectSaved(Project project) {

    }

    @Override
    public void projectClosed(Project project) {
        // Remove all the project-specific cards:
        removeContentPanel(CARD_KEYPAIR);
        removeContentPanel(CARD_UPDATE_SOURCES);
        removeContentPanel(CARD_VERSION_MANIFEST);
        removeContentPanel(CARD_JAR_SIGNING);
        removeContentPanel(CARD_UPLOAD);
    }
}
