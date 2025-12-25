package ca.corbett.packager.project;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.updates.VersionManifest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectManagerTest {

    private File projectDir;
    private static ProjectManager projectManager;

    @BeforeAll
    public static void initialize() {
        projectManager = ProjectManager.getInstance();
    }

    @BeforeEach
    public void setup() throws Exception {
        projectDir = new File(System.getProperty("java.io.tmpdir"), "test");
        projectManager.newProject("Test", projectDir);
    }

    @AfterEach
    public void tearDown() throws IOException {
        deleteDirectoryRecursively(projectDir);
    }

    @Test
    public void computeExtensionFilePath_givenValidRoot_shouldResolve() throws Exception {
        final File expected = new File(projectDir, "dist/test.txt");
        String path = "test.txt";
        File actual = projectManager.getProjectFileFromPath(path);
        assertNotNull(actual);
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }

    @Test
    public void computeExtensionFilePath_givenValidNonRoot_shouldResolve() throws Exception {
        final File expected = new File(projectDir, "dist/a/b/c/test.txt");
        String path = "a/b/c/test.txt";
        File actual = projectManager.computeExtensionFile(null, path);
        assertNotNull(actual);
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }

    @Test
    public void computeExtensionFilePath_givenExtension_shouldResolve() throws Exception {
        final File expected = new File(projectDir, "dist/extensions/1.0/MyExtension-1.0.0.jar");
        VersionManifest.ExtensionVersion version = new VersionManifest.ExtensionVersion();
        version.setExtInfo(new AppExtensionInfo.Builder("test")
                                   .setTargetAppName("Test")
                                   .setTargetAppVersion("1.0")
                                   .build());
        File actual = projectManager.computeExtensionFile(version, "MyExtension-1.0.0.jar");
        assertNotNull(actual);
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }

    @Test
    public void getBasename_returnsFilenameWithoutExtension() {
        assertEquals("hello", ProjectManager.getBasename("hello.txt"));
        assertEquals("hello.txt", ProjectManager.getBasename("hello.txt.txt"));
        assertEquals("hello", ProjectManager.getBasename("hello"));
        assertNull(ProjectManager.getBasename(null));
        assertEquals("  ", ProjectManager.getBasename("  "));
        assertEquals("hello", ProjectManager.getBasename("path/to/hello.txt"));
    }

    private static void deleteDirectoryRecursively(File rootDir) throws IOException {
        Path path = rootDir.toPath();
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     }
                     catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 });
        }
    }
}