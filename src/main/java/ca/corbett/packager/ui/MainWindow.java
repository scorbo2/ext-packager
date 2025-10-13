package ca.corbett.packager.ui;

import ca.corbett.packager.Version;

import javax.swing.JFrame;
import java.awt.Dimension;

public class MainWindow extends JFrame {

    private static MainWindow instance;

    private MainWindow() {
        super(Version.APPLICATION_NAME + " " + Version.VERSION);
        setSize(new Dimension(600,400));
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
        return instance;
    }
}
