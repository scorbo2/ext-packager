package ca.corbett.packager.io;

import ca.corbett.packager.project.ProjectManager;
import ca.corbett.packager.project.ProjectManagerTest;
import ca.corbett.updates.UpdateSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FtpParamsTest {

    private File projectDir;
    private static ProjectManager projectManager;
    private static UpdateSources.UpdateSource updateSource;

    @BeforeAll
    public static void initialize() throws Exception {
        projectManager = ProjectManager.getInstance();
        updateSource = new UpdateSources.UpdateSource("Test Source",
                                                      new URL("http://example.com/updates/"),
                                                      "user",
                                                      "pass");
    }

    @BeforeEach
    public void setup() throws Exception {
        // Create a new temporary project directory for each test so the tests don't interfere with each other:
        projectDir = new File(System.getProperty("java.io.tmpdir"), "projectManagerTest_" + System.currentTimeMillis());
        projectManager.newProject("Test", projectDir);
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Close and delete the project directory after each test:
        projectManager.close();
        ProjectManagerTest.deleteDirectoryRecursively(projectDir);
    }

    @Test
    public void fromUpdateSource_withNothingSaved_shouldReturnBlankInstance() throws Exception {
        FtpParams params = FtpParams.fromUpdateSource(projectManager.getProject(), updateSource);
        assertNotNull(params);
        assertEquals("", params.host);
        assertEquals("", params.username);
        assertEquals("", params.password);
        assertEquals("", params.targetDir);
    }

    @Test
    public void ftpParamsExist_withNothingSaved_shouldReturnFalse() {
        boolean exists = FtpParams.ftpParamsExist(projectManager.getProject(), updateSource);
        assertFalse(exists);
    }

    @Test
    public void load_withNothingSaved_shouldReturnBlankInstanceAndCreatePropsFile() throws Exception {
        FtpParams params = FtpParams.load(projectManager.getProject(), updateSource);
        assertNotNull(params);
        assertEquals("", params.host);
        assertEquals("", params.username);
        assertEquals("", params.password);
        assertEquals("", params.targetDir);

        // Props file should now exist:
        File propsFile = FtpParams.getPropsFile(projectManager.getProject(), updateSource);
        assertNotNull(propsFile);
        assertEquals(true, propsFile.exists());
        assertEquals("Test_Source.props", propsFile.getName());
    }

    @Test
    public void getPropsFile_shouldSanitizeFilename() {
        UpdateSources.UpdateSource sourceWithBadChars = new UpdateSources.UpdateSource("Source: With/Bad\\Chars*?",
                                                                                       null,
                                                                                       null,
                                                                                       null);
        File propsFile = FtpParams.getPropsFile(projectManager.getProject(), sourceWithBadChars);
        String expectedFilename = "Source__With_Bad_Chars__.props";
        assertEquals(expectedFilename, propsFile.getName());
    }

    @Test
    public void save_withValidData_shouldSave() throws Exception {
        FtpParams paramsToSave = new FtpParams();
        paramsToSave.host = "ftp.example.com";
        paramsToSave.username = "ftpuser";
        paramsToSave.password = "ftppass";
        paramsToSave.targetDir = "/uploads/";

        FtpParams.save(projectManager.getProject(), updateSource, paramsToSave);

        // Now load them back and verify:
        FtpParams loadedParams = FtpParams.load(projectManager.getProject(), updateSource);
        assertNotNull(loadedParams);
        assertEquals("ftp.example.com", loadedParams.host);
        assertEquals("ftpuser", loadedParams.username);
        assertEquals("ftppass", loadedParams.password);
        assertEquals("/uploads/", loadedParams.targetDir);
    }
}