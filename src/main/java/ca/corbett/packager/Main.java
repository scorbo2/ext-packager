package ca.corbett.packager;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.packager.ui.MainWindow;

import javax.swing.JFrame;
import java.awt.SplashScreen;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main entry point for the application. Currently just shows the UI, but
 * in future, a CLI may be offered here for batch scripting.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Main {

    public static void main(String[] args) {
        // Before we do anything else...
        initializeLogging();

        LookAndFeelManager.installExtraLafs();

        // Get the splash screen if there is one:
        final SplashScreen splashScreen = SplashScreen.getSplashScreen();

        // Load saved application config:
        Logger.getLogger(Main.class.getName())
              .info(Version.APPLICATION_NAME + " " + Version.VERSION + " initializing...");
        AppConfig.getInstance().load();
        LookAndFeelManager.switchLaf(AppConfig.getInstance().getLookAndFeelClassname());

        // Load and show main window:
        MainWindow window = MainWindow.getInstance();
        if (splashScreen != null) {
            try {
                // Wait a second or so, so it doesn't just flash up and disappear immediately.
                Thread.sleep(744);
            }
            catch (InterruptedException ignored) {
                // ignored
            }
            splashScreen.close();
        }

        // Create and display the form
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (args.length == 1) {
                    window.setStartupProjectFile(new File(args[0]));
                }
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.setVisible(true);
            }
        });
    }

    private static void initializeLogging() {
        // log file can be supplied as a system property:
        if (System.getProperties().containsKey("java.util.logging.config.file")) {
            // Do nothing. It will be used automatically.
        }

        // If it is not set, we'll assume it's in APPLICATION_HOME:
        else {
            File logProperties = new File(Version.SETTINGS_DIR, "logging.properties");
            if (logProperties.exists() && logProperties.canRead()) {
                try {
                    LogManager.getLogManager().readConfiguration(new FileInputStream(logProperties));
                }
                catch (IOException ioe) {
                    System.out.println("WARN: Unable to load log configuration from app dir: " + ioe.getMessage());
                }
            }

            // Otherwise, load our built-in default from jar resources:
            else {
                try {
                    LogManager.getLogManager().readConfiguration(
                            Main.class.getResourceAsStream("/ca/corbett/extpackager/logging.properties"));
                }
                catch (IOException ioe) {
                    System.out.println("WARN: Unable to load log configuration from jar: " + ioe.getMessage());
                }
            }
        }
    }
}
