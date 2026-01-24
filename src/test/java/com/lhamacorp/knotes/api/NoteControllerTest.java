package com.lhamacorp.knotes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhamacorp.knotes.api.dto.NoteRequest;
import com.lhamacorp.knotes.api.dto.NoteUpdateRequest;
import com.lhamacorp.knotes.domain.EncryptionMode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "encryption_key=test-pepper-for-integration-tests-must-be-long-enough-for-validation"
})
public class NoteControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    String timestamp = LocalDateTime.now().toString();
    private static final String TEST_PASSWORD = "secure-test-password-123";

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @DisplayName("Should Create An Empty Note")
    public void shouldCreateAnEmptyNote() throws JsonProcessingException {
        String id = createNoteAction(null);
        assertNotNull(id);
    }

    @Test
    @DisplayName("Should Create A Note With Content")
    public void shouldCreateANoteWithContent() throws IOException {
        String expectedContent = "Test " + timestamp;
        String id = createNoteAction(expectedContent);

        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent));
        cleanNoteAction(id);
    }

    @Test
    @DisplayName("Should Update A Note")
    public void shouldUpdateANote() throws IOException {
        String expectedContent = "Test " + timestamp;
        String updateContent = "Test Updated " + timestamp;

        String id = createNoteAction(expectedContent);
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent));

        updateNoteAction(id, updateContent);
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(updateContent));
        cleanNoteAction(id);
    }

    @Test
    @DisplayName("Should Find A Note By Id")
    public void shouldFindANoteById() throws IOException {
        String expectedContent = "Test " + timestamp;
        String id = createNoteAction(expectedContent);
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent));
        getANoteByIdAction(id, expectedContent);
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent));
        cleanNoteAction(id);
    }

    @Test
    @DisplayName("Should Check Updated Date")
    public void shouldCheckUpdatedDate() throws IOException {
        String expectedContent = "Test " + timestamp;
        String id = createNoteAction(expectedContent);

        given()
                .log().all()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent));
        String expectedUpdateDate1 = getUpdateDateAction(id);
        updateNoteAction(id, expectedContent);
        String expectedUpdateDate2 = getUpdateDateAction(id);
        Instant updateDate1 = Instant.parse(expectedUpdateDate1);
        Instant updateDate2 = Instant.parse(expectedUpdateDate2);
        assertTrue(updateDate2.isAfter(updateDate1));
    }

    @Test
    @DisplayName("Should Check Created Date")
    public void shouldCheckCreatedDate() throws IOException {
        String expectedContent = "Test " + timestamp;
        String id = createNoteAction(expectedContent);

        given()
                .log().all()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent));
        String expectedCreatedDate1 = getCreatedDateAction(id);
        updateNoteAction(id, expectedContent);
        String expectedCreatedDate2 = getCreatedDateAction(id);
        Instant updateDate1 = Instant.parse(expectedCreatedDate1);
        Instant updateDate2 = Instant.parse(expectedCreatedDate2);
        assertEquals(updateDate2, updateDate1);
    }

    @Test
    @DisplayName("Should Search For A Non-Existent ID")
    public void shouldSearchForNonExistentId() throws IOException {
        String id = "BLAF2TM3PBC6SK5ZG8NGS3YKFS";
        given()
                .log().all()
                .when()
                .get("/notes/" + id)
                .then()
                .log().all()
                .statusCode(404);
    }


    @Test
    @DisplayName("Should return 415 when sending plain text instead of JSON")
    public void shouldReturn415WhenContentTypeIsInvalid() {
        String invalidBody = "This is just a string, not a JSON";

        given()
                .log().all()
                .contentType("text/plain")
                .body(invalidBody)
                .when()
                .post("/notes")
                .then()
                .statusCode(415);
    }

    @Test
    @DisplayName("Should ensure data integrity after a failed update attempt")
    public void shouldMaintainOriginalContentAfterFailedUpdate() throws IOException {
        String originalContent = "Original Content " + timestamp;
        String id = createNoteAction(originalContent);
        String malformedJson = "{ \"content\": \"Updated Content\" ";

        given()
                .log().all()
                .contentType(JSON)
                .body(malformedJson)
                .when()
                .put("/notes/" + id)
                .then()
                .log().all()
                .statusCode(400);
        getANoteByIdAction(id, originalContent);
        cleanNoteAction(id);
    }

    @Test
    @DisplayName("Should handle real concurrent updates using multiple threads")
    public void shouldHandleRealConcurrentUpdates() throws IOException, InterruptedException {
        String id = createNoteAction("Initial State");
        String updateA = "Update A " + timestamp;
        String updateB = "Update B " + timestamp;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            try {
                latch.await();
                updateNoteAction(id, updateA);
            } catch (Exception e) { e.printStackTrace(); }
        });

        executor.execute(() -> {
            try {
                latch.await();
                updateNoteAction(id, updateB);
            } catch (Exception e) { e.printStackTrace(); }
        });

        latch.countDown();
        executor.shutdown();

        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Threads did not finish within the timeout period");

        Response response = given()
                .when()
                .get("/notes/" + id)
                .then()
                .extract()
                .response();

        String finalContent = response.path("content");
        String expectedWinner = finalContent.contains("Update A") ? updateA : updateB;

        assertNotEquals("Initial State", finalContent,
                "The content should have changed from the initial state");
        assertEquals(expectedWinner, finalContent,
                "The final content does not match the expected winner of the race");
    }

    @Test
    @DisplayName("Should persist accented characters and technical symbols correctly")
    public void shouldHandleAccentedCharactersAndSymbols() throws IOException {
        String complexContent = "Latin chars: áéíóú çãõ ñ | Technical symbols: @#$%^&*()_+-=[]{}|\\";

        String id = createNoteAction(complexContent);

        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(complexContent));
        cleanNoteAction(id);
    }

    @Test
    @DisplayName("Should return 404 when updating a non-existent content")
    public void shouldReturn404OnUpdateNonExistent() throws JsonProcessingException {
        String nonExistentId = "00000000000000000000000000";

        given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(new NoteUpdateRequest("some content", "PUBLIC")))
                .when()
                .put("/notes/" + nonExistentId)
                .then()
                .statusCode(404);
    }

    private String createNoteAction(String note) throws JsonProcessingException {
        return given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(new NoteRequest(note)))
                .when()
                .post("/notes")
                .then()
                .log().all()
                .statusCode(200)
                .extract()
                .path("id");
    }


    private void updateNoteAction(String id, String content) throws IOException {
        given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(new NoteUpdateRequest(content, "PUBLIC")))
                .when()
                .put("/notes/" + id)
                .then()
                .log().all()
                .statusCode(200)
                .body("content", equalTo(content));
    }

    private void cleanNoteAction(String id) throws IOException {
        updateNoteAction(id, "");
    }

    private void getANoteByIdAction(String id, String content) throws IOException {
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .log().all()
                .statusCode(200)
                .body("content", equalTo(content));
    }

    private String getUpdateDateAction(String id) throws IOException {
        return given()
                .get("/notes/" +id)
                .then()
                .statusCode(200)
                .extract()
                .path("modifiedAt");
    }

    private String getCreatedDateAction(String id) throws IOException {
        return given()
                .get("/notes/" +id)
                .then()
                .statusCode(200)
                .extract()
                .path("createdAt");
    }

    // ===== ENCRYPTION INTEGRATION TESTS =====

    @Test
    @DisplayName("Should create PUBLIC content with encryption metadata")
    public void shouldCreatePublicNoteWithEncryptionMetadata() throws JsonProcessingException {
        String expectedContent = "Public content content " + timestamp;
        NoteRequest request = new NoteRequest(expectedContent);

        String id = given()
                .contentType(JSON)
                .queryParam("encryptionMode", "PUBLIC")
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent))
                .body("encryptionMode", equalTo("PUBLIC"))
                .body("requiresPassword", equalTo(false))
                .extract()
                .path("id");

        // Verify retrieval works without any authentication
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent))
                .body("encryptionMode", equalTo("PUBLIC"))
                .body("requiresPassword", equalTo(false));
    }

    @Test
    @DisplayName("Should create PRIVATE content with owner encryption")
    public void shouldCreatePrivateNoteWithOwnerEncryption() throws JsonProcessingException {
        String expectedContent = "Private content content " + timestamp;
        NoteRequest request = new NoteRequest(expectedContent);

        String id = given()
                .contentType(JSON)
                .queryParam("encryptionMode", "PRIVATE")
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent)) // Content should be decrypted in response
                .body("encryptionMode", equalTo("PRIVATE"))
                .body("requiresPassword", equalTo(false))
                .extract()
                .path("id");

        // Verify retrieval works for owner
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent))
                .body("encryptionMode", equalTo("PRIVATE"))
                .body("requiresPassword", equalTo(false));
    }

    @Test
    @DisplayName("Should default to PRIVATE mode for authenticated users when no encryptionMode is specified")
    public void shouldDefaultToPrivateModeForAuthenticatedUsers() throws JsonProcessingException {
        String expectedContent = "Default encryption behavior test " + timestamp;
        NoteRequest request = new NoteRequest(expectedContent);

        String id = given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent))
                .body("encryptionMode", equalTo("PRIVATE")) // Should default to PRIVATE for authenticated users
                .body("requiresPassword", equalTo(false))
                .extract()
                .path("id");

        // Verify the content can be retrieved by the owner
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent))
                .body("encryptionMode", equalTo("PRIVATE"));
    }

//    @Test
//    @DisplayName("Should create PASSWORD_SHARED content with password encryption")
//    public void shouldCreatePasswordSharedNoteWithPasswordEncryption() throws JsonProcessingException {
//        String expectedContent = "Password protected content content " + timestamp;
//        NoteRequest request = new NoteRequest(expectedContent);
//
//        String id = given()
//                .contentType(JSON)
//                .queryParam("encryptionMode", "PASSWORD_SHARED")
//                .queryParam("password", TEST_PASSWORD)
//                .body(objectMapper.writeValueAsString(request))
//                .when()
//                .post("/notes")
//                .then()
//                .statusCode(200)
//                .body("content", equalTo(expectedContent)) // Content should be decrypted in response
//                .body("encryptionMode", equalTo("PASSWORD_SHARED"))
//                .body("requiresPassword", equalTo(true))
//                .extract()
//                .path("id");
//
//        // Verify retrieval works with correct password
//        given()
//                .param("password", TEST_PASSWORD)
//                .when()
//                .get("/notes/" + id)
//                .then()
//                .statusCode(200)
//                .body("content", equalTo(expectedContent))
//                .body("encryptionMode", equalTo("PASSWORD_SHARED"))
//                .body("requiresPassword", equalTo(true));
//    }

//    @Test
//    @DisplayName("Should reject PASSWORD_SHARED content creation without password")
//    public void shouldRejectPasswordSharedNoteWithoutPassword() throws JsonProcessingException {
//        String expectedContent = "This should fail " + timestamp;
//        NoteRequest request = new NoteRequest(expectedContent);
//
//        given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(request))
//                .when()
//                .post("/notes")
//                .then()
//                .statusCode(400); // Should fail validation
//    }

//    @Test
//    @DisplayName("Should reject PASSWORD_SHARED content retrieval without password")
//    public void shouldRejectPasswordSharedNoteRetrievalWithoutPassword() throws JsonProcessingException {
//        String expectedContent = "Password required content " + timestamp;
//        NoteRequest request = new NoteRequest(expectedContent);
//
//        String id = given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(request))
//                .when()
//                .post("/notes")
//                .then()
//                .statusCode(200)
//                .extract()
//                .path("id");
//
//        // Attempt retrieval without password should fail
//        given()
//                .when()
//                .get("/notes/" + id)
//                .then()
//                .statusCode(400); // Should fail with DecryptionException
//    }

//    @Test
//    @DisplayName("Should reject PASSWORD_SHARED content retrieval with wrong password")
//    public void shouldRejectPasswordSharedNoteWithWrongPassword() throws JsonProcessingException {
//        String expectedContent = "Password required content " + timestamp;
//        NoteRequest request = new NoteRequest(expectedContent);
//
//        String id = given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(request))
//                .when()
//                .post("/notes")
//                .then()
//                .statusCode(200)
//                .extract()
//                .path("id");
//
//        // Attempt retrieval with wrong password should fail
//        given()
//                .param("password", "wrong-password")
//                .when()
//                .get("/notes/" + id)
//                .then()
//                .statusCode(400); // Should fail with DecryptionException
//    }

//    @Test
//    @DisplayName("Should update content encryption mode from PUBLIC to PRIVATE")
//    public void shouldUpdateNoteFromPublicToPrivate() throws JsonProcessingException {
//        String originalContent = "Original public content " + timestamp;
//        String updatedContent = "Updated private content " + timestamp;
//
//        // Create public content
//        String id = createNoteAction(originalContent);
//
//        // Update to private with new content
//        NoteUpdateRequest updateRequest = new NoteUpdateRequest(updatedContent, "PRIVATE");
//
//        given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(updateRequest))
//                .when()
//                .put("/notes/" + id)
//                .then()
//                .statusCode(200)
//                .body("content", equalTo(updatedContent))
//                .body("encryptionMode", equalTo("PRIVATE"))
//                .body("requiresPassword", equalTo(false));
//
//        // Verify the content is now private
//        given()
//                .when()
//                .get("/notes/" + id)
//                .then()
//                .statusCode(200)
//                .body("content", equalTo(updatedContent))
//                .body("encryptionMode", equalTo("PRIVATE"));
//    }

//    @Test
//    @DisplayName("Should update content encryption mode from PRIVATE to PASSWORD_SHARED")
//    public void shouldUpdateNoteFromPrivateToPasswordShared() throws JsonProcessingException {
//        String originalContent = "Original private content " + timestamp;
//        String updatedContent = "Updated password shared content " + timestamp;
//
//        // Create private content
//        NoteRequest createRequest = new NoteRequest(originalContent);
//        String id = given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(createRequest))
//                .when()
//                .post("/notes")
//                .then()
//                .statusCode(200)
//                .extract()
//                .path("id");
//
//        // Update to password shared with new content
//        NoteUpdateRequest updateRequest = new NoteUpdateRequest(updatedContent, EncryptionMode.PASSWORD_SHARED, TEST_PASSWORD);
//
//        given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(updateRequest))
//                .when()
//                .put("/notes/" + id)
//                .then()
//                .statusCode(200)
//                .body("content", equalTo(updatedContent))
//                .body("encryptionMode", equalTo("PASSWORD_SHARED"))
//                .body("requiresPassword", equalTo(true));
//
//        // Verify retrieval now requires password
//        given()
//                .param("password", TEST_PASSWORD)
//                .when()
//                .get("/notes/" + id)
//                .then()
//                .statusCode(200)
//                .body("content", equalTo(updatedContent))
//                .body("encryptionMode", equalTo("PASSWORD_SHARED"))
//                .body("requiresPassword", equalTo(true));
//    }

    @Test
    @DisplayName("Should handle metadata endpoint with encryption information")
    public void shouldReturnMetadataWithEncryptionInfo() throws JsonProcessingException {
        String expectedContent = "Content for metadata test " + timestamp;
        NoteRequest request = new NoteRequest(expectedContent);

        String id = given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // Verify metadata includes encryption information
        given()
                .when()
                .get("/notes/" + id + "/metadata")
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("encryptionMode", equalTo("PRIVATE"))
                .body("requiresPassword", equalTo(false))
                .body("createdAt", notNullValue())
                .body("modifiedAt", notNullValue());
    }

    @Test
    @DisplayName("Should handle backward compatibility with existing notes (default PUBLIC)")
    public void shouldHandleBackwardCompatibilityWithPublicDefault() throws JsonProcessingException {
        String expectedContent = "Backward compatibility test " + timestamp;

        // Create content using old format (no encryption parameters)
        String id = createNoteAction(expectedContent);

        // Verify content defaults to PUBLIC mode
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(expectedContent))
                .body("encryptionMode", equalTo("PUBLIC"))
                .body("requiresPassword", equalTo(false));
    }

    @Test
    @DisplayName("Should handle empty content with encryption")
    public void shouldHandleEmptyContentWithEncryption() throws JsonProcessingException {
        String emptyContent = "";
        NoteRequest request = new NoteRequest(emptyContent);

        String id = given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .body("content", equalTo(emptyContent))
                .body("encryptionMode", equalTo("PRIVATE"))
                .extract()
                .path("id");

        // Verify empty encrypted content can be retrieved
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(emptyContent))
                .body("encryptionMode", equalTo("PRIVATE"));
    }

    @Test
    @DisplayName("Should handle special characters with encryption")
    public void shouldHandleSpecialCharactersWithEncryption() throws JsonProcessingException {
        String complexContent = "Encrypted special chars: áéíóú çãõ ñ | Symbols: @#$%^&*()_+-=[]{}|\\";
        NoteRequest request = new NoteRequest(complexContent);

        String id = given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .body("content", equalTo(complexContent))
                .extract()
                .path("id");

        // Verify special characters survive encryption/decryption
        given()
                .param("password", TEST_PASSWORD)
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(complexContent));
    }

//    @Test
//    @DisplayName("Should validate encryption mode in update requests")
//    public void shouldValidateEncryptionModeInUpdateRequests() throws JsonProcessingException {
//        String originalContent = "Original content " + timestamp;
//        String updatedContent = "Updated content " + timestamp;
//
//        // Create public content
//        String id = createNoteAction(originalContent);
//
//        // Try to update to PASSWORD_SHARED without password
//        NoteUpdateRequest updateRequest = new NoteUpdateRequest(updatedContent, EncryptionMode.PASSWORD_SHARED, null);
//
//        given()
//                .contentType(JSON)
//                .body(objectMapper.writeValueAsString(updateRequest))
//                .when()
//                .put("/notes/" + id)
//                .then()
//                .statusCode(400); // Should fail validation
//    }

    @Test
    @DisplayName("Should maintain encryption when updating content only")
    public void shouldMaintainEncryptionWhenUpdatingContentOnly() throws IOException {
        String originalContent = "Original encrypted content " + timestamp;
        String updatedContent = "Updated encrypted content " + timestamp;

        // Create private content
        NoteRequest createRequest = new NoteRequest(originalContent);
        String id = given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(createRequest))
                .when()
                .post("/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // Update content only (using old format)
        updateNoteAction(id, updatedContent);

        // Verify encryption mode is preserved
        given()
                .when()
                .get("/notes/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo(updatedContent))
                .body("encryptionMode", equalTo("PRIVATE"))
                .body("requiresPassword", equalTo(false));
    }

}