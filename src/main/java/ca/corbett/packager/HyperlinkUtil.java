package ca.corbett.packager;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Utility class for hyperlink launching in JREs that support it.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since 1.2
 */
public class HyperlinkUtil {

    private static final Logger log = Logger.getLogger(HyperlinkUtil.class.getName());
    private static final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

    private HyperlinkUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Some JREs don't allow launching a hyperlink to open the browser.
     */
    public static boolean isBrowsingSupported() {
        return desktop != null && desktop.isSupported(Desktop.Action.BROWSE);
    }

    /**
     * If hyperlink launching is supported, will open the user's default browser to the
     * given link (assuming the link is valid). If the JRE doesn't allow such things,
     * then the given link will be copied to the clipboard instead (better than nothing).
     * No message dialog will be shown on failure. If you wish to show a message dialog
     * on failure, use the overload that accepts an owner Component.
     */
    public static void openHyperlink(String link) {
        openHyperlink(link, null);
    }

    /**
     * If hyperlink launching is supported, will open the user's default browser to the
     * given link (assuming the link is valid). If the JRE doesn't allow such things,
     * then the given link will be copied to the clipboard instead (better than nothing).
     * If an owner Component is provided, a message dialog will be shown to inform
     * the user that the link was copied to the clipboard.
     */
    public static void openHyperlink(String link, Component owner) {
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
            if (owner != null) {
                JOptionPane.showMessageDialog(owner, "Hyperlinks are not enabled in your JRE.\n"
                                              + "Link copied to clipboard instead.",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                log.info("Hyperlinks are not enabled in your JRE. Link copied to clipboard instead.");
            }
        }
    }
}
