package ca.corbett.packager.project;

import ca.corbett.updates.UpdateSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectManagerTest {

    File projectDir;
    UpdateSources updateSources;

    @BeforeEach
    public void setup() throws Exception {
        projectDir = new File(System.getProperty("java.io.tmpdir"), "test");
        ProjectManager.getInstance().newProject("Test", projectDir);

        updateSources = new UpdateSources("Test");
        updateSources.addUpdateSource(new UpdateSources.UpdateSource("Test",
                                                                     new URL("http://www.test.example"),
                                                                     "version_manifest.json"));
        ProjectManager.getInstance().getProject().setUpdateSources(updateSources);
    }

    @AfterEach
    public void tearDown() throws IOException {
        deleteDirectoryRecursively(projectDir);
    }

    @Test
    public void getProjectFileFromURL_givenValidURL_shouldResolve() throws Exception {
        final File expected = new File(projectDir, "dist/test.txt");
        URL url = new URL("http://www.test.example/test.txt");
        File actual = ProjectManager.getInstance().getProjectFileFromURL(updateSources.getUpdateSources().get(0), url);
        assertNotNull(actual);
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }

    @Test
    public void getURLFromProjectFile_givenValidRootFile_shouldResolve() throws Exception {
        final URL expected = new URL("http://www.test.example/test.txt");
        File file = new File(projectDir, "dist/test.txt");
        URL actual = ProjectManager.getInstance().getURLFromProjectFile(updateSources.getUpdateSources().get(0), file);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void getURLFromProjectFile_givenValidNonRootFile_shouldResolve() throws Exception {
        final URL expected = new URL("http://www.test.example/a/b/c/test.txt");
        File file = new File(projectDir, "dist/a/b/c/test.txt");
        URL actual = ProjectManager.getInstance().getURLFromProjectFile(updateSources.getUpdateSources().get(0), file);
        assertNotNull(actual);
        assertEquals(expected, actual);
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