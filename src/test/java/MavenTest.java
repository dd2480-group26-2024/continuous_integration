import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import org.json.JSONObject;

import org.mockito.Mockito;
import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.util.HashMap;
import java.io.File; 

public class MavenTest {

    @Test
    public void trueTest() {
        assertEquals(1, 1);
    }

    @Test
    public void testCloneAndCheckout(@TempDir Path tempDir) {//TempDir is a junit thing that creates a temporary directory in /tmp folder 
        // Arrange
        ContinuousIntegration ci = new ContinuousIntegration();
        String repoUrl = "https://github.com/dd2480-group26-2024/decide"; // replace with your repo URL
        String commitId = "6cdd0d9b6d2d60956e9bb4750d4f1431219a5616"; // replace with a commit id to test
        String directoryPath = tempDir.toString(); // Use JUnit's @TempDir feature to create a temporary directory

        try {
            // Act
            ci.cloneAndCheckout(repoUrl, commitId, directoryPath);

            // Assert
            Path repoPath = tempDir.resolve(".git"); // Path to the .git directory
            assertTrue(Files.exists(repoPath), "Repository .git directory should exist after cloning");

            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            try (Repository repository = repositoryBuilder.setGitDir(repoPath.toFile()).readEnvironment().findGitDir().build();
                RevWalk revWalk = new RevWalk(repository)) {
                ObjectId lastCommitId = repository.resolve(commitId);
                RevCommit revCommit = revWalk.parseCommit(lastCommitId);
                System.out.println("");
                assertEquals(commitId, revCommit.getId().getName(), "The checked out commit ID should match the requested commit ID.");
            }

            //JUnit cleans up the tempdir automatically!
        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }
	
	@Test
	public void testProcessRequestData(){
		ContinuousIntegration ci = new ContinuousIntegration();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		// Simplified GitHub request payload
		String payload = "{\"repository\": {\"clone_url\": \"https://github.com/dd2480-group26-2024/continuous_integration.git\" , \"name\": \"repo name\"},\"head_commit\": {\"id\": \"545c38c57a26677c764a657fb42f2186c34c8bac\",\"message\": \"edited the sendEmailNotification method and removed the newly added method\",\"timestamp\": \"2024-02-12T16:47:59+01:00\",\"committer\": {\"email\": \"robin.yurt@hotmail.com\",}}}";
		HashMap<String,String> expected = new HashMap<>();			
		expected.put("clone_url", "https://github.com/dd2480-group26-2024/continuous_integration.git"); 
		expected.put("commit_id", "545c38c57a26677c764a657fb42f2186c34c8bac"); 
		expected.put("email", "robin.yurt@hotmail.com"); 
		expected.put("timestamp", "2024-02-12T16:47:59+01:00"); 
		expected.put("repo_name", "repo name"); 
		expected.put("commit_message", "edited the sendEmailNotification method and removed the newly added method"); 
		Mockito.when(request.getParameter("payload")).thenReturn(payload);
		HashMap<String,String> result = ci.processRequestData(request);
		assertTrue(result.equals(expected));
	}


    //"src/test/testProject"
    @Test
    public void testRepoTesting(){
        // run tests in new directory
        ContinuousIntegration ci = new ContinuousIntegration();
        try{
            boolean res = ci.runTests("src/test/TestMavenProject/testProject", new ByteArrayOutputStream());
            assertTrue(res);
        }catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }

    }

    @Test
    public void testRepoTestingFailure(){
        // run tests in new directory
        ContinuousIntegration ci = new ContinuousIntegration();
        try{
            boolean res = ci.runTests("src/test/TestMavenProject/testProjectFailure", new ByteArrayOutputStream());
            assertFalse(res);
        }catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }

    }


    @Test
    public void testUpdateGitHubStatus() throws Exception {

        HttpClient httpClientMock = mock(HttpClient.class);
        HttpResponse<Object> httpResponseMock = mock(HttpResponse.class);

        when(httpResponseMock.body()).thenReturn("{\"state\": \"success\"}");
        when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(httpResponseMock);

        ContinuousIntegration ci = new ContinuousIntegration(httpClientMock);

        String result = ci.updateGitHubStatus(true, "abc123", "Passed");

        assertEquals("success", result);

        verify(httpClientMock).send(any(HttpRequest.class), any());
    }

    @Test
    public void test_compile_project_true() {
        try {
            ContinuousIntegration ci = new ContinuousIntegration();

            // Try to compile the correct project
            boolean comp=ci.compileMavenProject("src/test/TestMavenProject/mvnProjectCorrect");
            assertTrue(comp);

        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }
    @Test
    public void test_compile_project_false() {
        try {
            ContinuousIntegration ci = new ContinuousIntegration();

            // Try to compile the incorrect project
            boolean comp=ci.compileMavenProject("src/test/TestMavenProject/mvnProjectIncorrect");
            assertFalse(comp);

        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }
	
	@Test
	public void sendEmailNotificationBuildSuccess() {
		ContinuousIntegration ci = new ContinuousIntegration();
		
		// Mock the request data
		HashMap<String, String> requestData = new HashMap<>();
		requestData.put("commit_id", "ecff3ba2c436e2fa743b149d33b906ed74370620");
		requestData.put("clone_url", "https://github.com/robinho46/sendMailTestRepo.git");
		requestData.put("email", "group26kth@gmail.com"); // Replace with the recipient's email address

		// Call the method and assert the result
		assertTrue(ci.sendEmailNotification(requestData, true));
	}
	
	@Test
	public void sendEmailNotificationBuildFailure(){
		ContinuousIntegration ci = new ContinuousIntegration();
		
		// Mock the request data
		HashMap<String, String> requestData = new HashMap<>();
		requestData.put("commit_id", "ecff3ba2c436e2fa743b149d33b906ed74370620");
		requestData.put("clone_url", "https://github.com/robinho46/sendMailTestRepo.git");
		requestData.put("email", "group26kth@gmail.com"); // Replace with the recipient's email address

		// Call the method and assert the result
		assertTrue(ci.sendEmailNotification(requestData, false));
	}
	
	@Test
	public void sendEmailNotificationBuildWithCommitMessage(){
		ContinuousIntegration ci = new ContinuousIntegration();
		
		// Mock the request data
		HashMap<String, String> requestData = new HashMap<>();
		requestData.put("commit_id", "ecff3ba2c436e2fa743b149d33b906ed74370620");
		requestData.put("clone_url", "https://github.com/robinho46/sendMailTestRepo.git");
		requestData.put("email", "group26kth@gmail.com"); // Replace with the recipient's email address
		requestData.put("commit_message", "added a main.cpp file for fun :D");
		
		// Call the method and assert the result
		assertTrue(ci.sendEmailNotification(requestData, false));
	}



	@TempDir
	Path buildHistDir;
	@Test
	public void testSaveToBuildHistory(){
		Path buildDir = buildHistDir.resolve("builds");
		Path templatePath = buildHistDir.resolve("builds/_template.html");
		Path indexPath = buildHistDir.resolve("index.html");
		try {
			Files.createDirectories(buildDir);
            Files.createFile(templatePath);
            Files.createFile(indexPath);
			byte[] indexHtml = ("<!doctype html> <html lang=\"en\">  <head>   <meta charset=\"UTF-8\">   <title>Build History</title>  </head>  <body>   <h1>Build History</h1>   <br>  </body> </html>").getBytes();
			byte[] templateHtml = ("<!doctype html> <html lang=\"en\">  <head>   <meta charset=\"UTF-8\">   <title>Build History</title>  </head>  <body>   <h1>Build Information</h1>   <p>Commit Hash: $commit_id</p>   <p>Build Date: $build_date</p>   <h1>Build Logs</h1>   <div> 	<textarea rows=\"30\" wrap=\"off\" style=\"width: 90%; overflow-x: auto;\">$build_logs</textarea>   </div>  </body> </html> ").getBytes();
			Files.write(indexPath, indexHtml);
			Files.write(templatePath, templateHtml);
        } catch (IOException e) {
			fail("Test failed due to exception: " + e.getMessage());
        }
		ContinuousIntegration ci = new ContinuousIntegration();
		assertTrue(ci.saveToBuildHistory("COMMIT_ID", "Some logs\n on multiple\n lines", "2024-02-13", buildHistDir.toString()));
	}
}
