package ca.corbett.packager;

import ca.corbett.extensions.AppExtension;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extensions.AppProperties;
import ca.corbett.extensions.ExtensionManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import com.formdev.flatlaf.FlatDarkLaf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppConfig extends AppProperties<AppConfig.NullExtension> {

    private static AppConfig instance;

    private LookAndFeelProperty lookAndFeelProp;
    private DirectoryProperty projectBaseDirProp;

    private AppConfig() {
        super(Version.APPLICATION_NAME + " " + Version.VERSION,
              Version.APP_CONFIG_FILE,
              new NullExtensionManager());
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public String getLookAndFeelClassname() {
        return lookAndFeelProp.getSelectedLafClass();
    }

    public LookAndFeelProperty getLookAndFeelProp() {
        return lookAndFeelProp;
    }

    public File getProjectBaseDir() {
        return projectBaseDirProp.getDirectory();
    }

    public void setProjectBaseDir(File newDir) {
        projectBaseDirProp.setDirectory(newDir);
    }

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        lookAndFeelProp = new LookAndFeelProperty("UI.Look and Feel.Look and Feel",
                                                  "Look and Feel:",
                                                  FlatDarkLaf.class.getName());
        props.add(lookAndFeelProp);

        projectBaseDirProp = new DirectoryProperty("General.General.projectBaseDir",
                                                   "Project base dir:",
                                                   true,
                                                   null);
        props.add(projectBaseDirProp);

        return props;
    }

    public static class NullExtension extends AppExtension {

        @Override
        public AppExtensionInfo getInfo() {
            return null;
        }

        @Override
        protected List<AbstractProperty> createConfigProperties() {
            return List.of();
        }

        @Override
        public void loadJarResources() {
        }
    }

    private static class NullExtensionManager extends ExtensionManager<NullExtension> {

    }
}
