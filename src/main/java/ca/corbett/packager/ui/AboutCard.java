package ca.corbett.packager.ui;

import ca.corbett.extras.about.AboutPanel;
import ca.corbett.forms.Alignment;
import ca.corbett.packager.Version;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class AboutCard extends JPanel {

    public AboutCard() {
        setLayout(new BorderLayout());
        add(new AboutPanel(Version.aboutInfo, Alignment.TOP_LEFT, 12), BorderLayout.CENTER);
    }
}
