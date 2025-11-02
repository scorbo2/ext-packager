package ca.corbett.packager.ui;

import ca.corbett.extras.about.AboutPanel;
import ca.corbett.forms.Alignment;
import ca.corbett.packager.Version;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * A simple card to show About information for this ext-packager application.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class AboutCard extends JPanel {

    public AboutCard() {
        setLayout(new BorderLayout());
        add(new AboutPanel(Version.aboutInfo, Alignment.TOP_LEFT, 12), BorderLayout.CENTER);
    }
}
