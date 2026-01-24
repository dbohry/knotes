package com.lhamacorp.knotes.domain;

import com.lhamacorp.knotes.exception.DecryptionException;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Note Domain Model Tests")
class NoteTest {

    private static final String TEST_CONTENT = "This is a test content with some content for encryption testing.";
    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_PASSWORD = "secure-password-123";
    private static final String TEST_KEY = "test-application-key-for-key-derivation-security";

    @BeforeEach
    void setUp() {
        // Set up test key for encryption tests
        System.setProperty("knotes.encryption.key", TEST_KEY);
    }

    // ===== EXISTING TESTS (Backward Compatibility) =====

    @Test
    @DisplayName("Constructor with string content should compress and store (backward compatibility)")
    void constructor_withStringContent_shouldCompressAndStore() {
        // Given
        String id = "test-id";
        String content = "This is a test content with some content that should be compressed.";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, content, null, now, now);

        // Then
        assertEquals(id, note.id());
        assertEquals(content, note.content());
        assertEquals(now, note.createdAt());
        assertEquals(now, note.modifiedAt());

        // Verify that content is actually stored compressed
        assertNotNull(note.compressedData());
        assertTrue(note.compressedData().getData().length > 0);

        // Verify encryption defaults for backward compatibility
        assertEquals(EncryptionMode.PUBLIC, note.encryptionMode());
        assertNull(note.encryptionSalt());
        assertFalse(note.requiresPassword());
    }

    @Test
    @DisplayName("Constructor with null content should handle gracefully")
    void constructor_withNullContent_shouldHandleGracefully() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, (String) null, null, now, now);

        // Then
        assertEquals(id, note.id());
        assertNull(note.content());
        assertNull(note.compressedData());
    }

    @Test
    @DisplayName("Content with compressed data should decompress correctly")
    void content_withCompressedContent_shouldDecompressCorrectly() {
        // Given
        String originalContent = "Test content content for compression verification.";
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, originalContent, null, now, now);
        String retrievedContent = note.content();

        // Then
        assertEquals(originalContent, retrievedContent);
    }

    // ===== NEW ENCRYPTION TESTS =====

    @Test
    @DisplayName("Should create PUBLIC content with no encryption (default)")
    void constructor_withPublicMode_shouldNotEncrypt() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PUBLIC, null);

        // Then
        assertEquals(id, note.id());
        assertEquals(TEST_CONTENT, note.content());
        assertEquals(TEST_USER_ID, note.createdBy());
        assertEquals(EncryptionMode.PUBLIC, note.encryptionMode());
        assertNull(note.encryptionSalt());
        assertFalse(note.requiresPassword());

        // Content should be retrievable without authentication
        assertEquals(TEST_CONTENT, note.content(null, null));
    }

    @Test
    @DisplayName("Should create PRIVATE content with owner encryption")
    void constructor_withPrivateMode_shouldEncryptForOwner() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PRIVATE, null);

        // Then
        assertEquals(id, note.id());
        assertEquals(TEST_USER_ID, note.createdBy());
        assertEquals(EncryptionMode.PRIVATE, note.encryptionMode());
        assertNotNull(note.encryptionSalt());
        assertFalse(note.requiresPassword());

        // Content should be retrievable by owner
        assertEquals(TEST_CONTENT, note.content(TEST_USER_ID, null));

        // Verify content is actually encrypted (compressed data should be different from original)
        assertNotNull(note.compressedData());
        // The encrypted data will be longer than just compressed data due to IV + auth tag
    }

    @Test
    @DisplayName("Should create PASSWORD_SHARED content with password encryption")
    void constructor_withPasswordSharedMode_shouldEncryptWithPassword() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PASSWORD_SHARED, TEST_PASSWORD);

        // Then
        assertEquals(id, note.id());
        assertEquals(TEST_USER_ID, note.createdBy());
        assertEquals(EncryptionMode.PASSWORD_SHARED, note.encryptionMode());
        assertNotNull(note.encryptionSalt());
        assertTrue(note.requiresPassword());

        // Content should be retrievable with correct password
        assertEquals(TEST_CONTENT, note.content(null, TEST_PASSWORD));
        // Owner can also access with password
        assertEquals(TEST_CONTENT, note.content(TEST_USER_ID, TEST_PASSWORD));
    }

    @Test
    @DisplayName("Should reject PASSWORD_SHARED mode without password")
    void constructor_withPasswordSharedModeNoPassword_shouldThrow() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PASSWORD_SHARED, null);
        });

        assertTrue(exception.getMessage().contains("Password required for PASSWORD_SHARED mode"));
    }

    @Test
    @DisplayName("Should force PUBLIC mode for anonymous user")
    void constructor_anonymousUser_shouldForcePublicMode() {
        // Given
        String id = "test-id";
        String anonymousUserId = "1";
        Instant now = Instant.now();

        // When - try to create private content as anonymous user
        Note note = new Note(id, TEST_CONTENT, anonymousUserId, now, now, EncryptionMode.PRIVATE, null);

        // Then - should be forced to PUBLIC mode
        assertEquals(EncryptionMode.PUBLIC, note.encryptionMode());
        assertNull(note.encryptionSalt());
        assertFalse(note.requiresPassword());
        assertEquals(TEST_CONTENT, note.content());
    }

    @Test
    @DisplayName("Should reject unauthorized access to PRIVATE content")
    void content_privateNoteWrongUser_shouldThrowUnauthorized() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PRIVATE, null);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            note.content("different-user", null);
        });

        assertTrue(exception.getMessage().contains("Not authorized to decrypt this content"));
    }

    @Test
    @DisplayName("Should reject PASSWORD_SHARED content access without password")
    void content_passwordSharedNoPassword_shouldThrowDecryptionException() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PASSWORD_SHARED, TEST_PASSWORD);

        // When & Then
        DecryptionException exception = assertThrows(DecryptionException.class, () -> {
            note.content(null, null);
        });

        assertTrue(exception.getMessage().contains("Password required to decrypt this content"));
    }

    @Test
    @DisplayName("Should reject PASSWORD_SHARED content access with wrong password")
    void content_passwordSharedWrongPassword_shouldThrowDecryptionException() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PASSWORD_SHARED, TEST_PASSWORD);

        // When & Then
        DecryptionException exception = assertThrows(DecryptionException.class, () -> {
            note.content(null, "wrong-password");
        });

        assertTrue(exception.getMessage().contains("Failed to decrypt content"));
    }

    @Test
    @DisplayName("Should handle null encryption mode as PUBLIC (backward compatibility)")
    void constructor_nullEncryptionMode_shouldDefaultToPublic() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, null, null);

        // Then
        assertEquals(EncryptionMode.PUBLIC, note.encryptionMode());
        assertNull(note.encryptionSalt());
        assertFalse(note.requiresPassword());
        assertEquals(TEST_CONTENT, note.content());
    }

    @Test
    @DisplayName("Should handle encrypted content with missing salt gracefully")
    void content_encryptedNoteWithoutSalt_shouldThrowDecryptionException() {
        // Given - create a content with some content first, then manually corrupt it to simulate missing salt
        String id = "test-id";
        Instant now = Instant.now();

        // Create a valid encrypted content first
        Note validNote = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PRIVATE, null);

        // Create a corrupted content - copy the encrypted data but remove the salt (simulating corruption)
        Note corruptedNote = new Note(id, validNote.compressedData(), TEST_USER_ID, now, now,
                                      EncryptionMode.PRIVATE, null, false);

        // When & Then
        DecryptionException exception = assertThrows(DecryptionException.class, () -> {
            corruptedNote.content(TEST_USER_ID, null);
        });

        assertTrue(exception.getMessage().contains("Encryption metadata missing"));
    }

    @Test
    @DisplayName("Should encrypt different notes with same content differently (unique salts)")
    void constructor_sameContentDifferentNotes_shouldProduceDifferentEncryptedData() {
        // Given
        String id1 = "test-id-1";
        String id2 = "test-id-2";
        Instant now = Instant.now();

        // When
        Note note1 = new Note(id1, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PRIVATE, null);
        Note note2 = new Note(id2, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PRIVATE, null);

        // Then
        // Both should have different salts
        assertNotNull(note1.encryptionSalt());
        assertNotNull(note2.encryptionSalt());
        assertFalse(java.util.Arrays.equals(note1.encryptionSalt().getData(), note2.encryptionSalt().getData()));

        // Both should decrypt to the same content
        assertEquals(TEST_CONTENT, note1.content(TEST_USER_ID, null));
        assertEquals(TEST_CONTENT, note2.content(TEST_USER_ID, null));

        // But encrypted data should be different
        assertFalse(java.util.Arrays.equals(note1.compressedData().getData(), note2.compressedData().getData()));
    }

    @Test
    @DisplayName("Should handle empty content with encryption")
    void constructor_emptyContentWithEncryption_shouldWork() {
        // Given
        String id = "test-id";
        String emptyContent = "";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, emptyContent, TEST_USER_ID, now, now, EncryptionMode.PRIVATE, null);

        // Then
        assertEquals(EncryptionMode.PRIVATE, note.encryptionMode());
        assertNotNull(note.encryptionSalt());
        assertEquals(emptyContent, note.content(TEST_USER_ID, null));
    }

    @Test
    @DisplayName("Should preserve creation metadata through encryption constructors")
    void constructor_withEncryption_shouldPreserveMetadata() {
        // Given
        String id = "test-id";
        Instant createdAt = Instant.now().minusSeconds(3600); // 1 hour ago
        Instant modifiedAt = Instant.now();

        // When
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, createdAt, modifiedAt, EncryptionMode.PRIVATE, null);

        // Then
        assertEquals(id, note.id());
        assertEquals(TEST_USER_ID, note.createdBy());
        assertEquals(createdAt, note.createdAt());
        assertEquals(modifiedAt, note.modifiedAt());
        assertEquals(EncryptionMode.PRIVATE, note.encryptionMode());
    }

    @Test
    @DisplayName("Should support different users with same password for PASSWORD_SHARED notes")
    void content_passwordSharedDifferentUsers_shouldAllowAccess() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();
        Note note = new Note(id, TEST_CONTENT, TEST_USER_ID, now, now, EncryptionMode.PASSWORD_SHARED, TEST_PASSWORD);

        // When & Then
        // Original owner can access
        assertEquals(TEST_CONTENT, note.content(TEST_USER_ID, TEST_PASSWORD));

        // Different user can also access with password
        assertEquals(TEST_CONTENT, note.content("different-user", TEST_PASSWORD));

        // Anonymous user can access with password
        assertEquals(TEST_CONTENT, note.content("1", TEST_PASSWORD));
    }
}