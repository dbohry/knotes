package com.lhamacorp.knotes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhamacorp.knotes.api.dto.NoteRequest;
import com.lhamacorp.knotes.api.dto.NoteUpdateRequest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NoteControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    String timestamp = LocalDateTime.now().toString();

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
    @DisplayName("Should return 404 when updating a non-existent note")
    public void shouldReturn404OnUpdateNonExistent() throws JsonProcessingException {
        String nonExistentId = "00000000000000000000000000";

        given()
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(new NoteUpdateRequest("some content")))
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
                .body(objectMapper.writeValueAsString(new NoteUpdateRequest(content)))
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

}