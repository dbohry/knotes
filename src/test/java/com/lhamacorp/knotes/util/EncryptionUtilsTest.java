package com.lhamacorp.knotes.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EncryptionUtils Tests")
class EncryptionUtilsTest {

    private static final String TEST_CONTENT = "This is a test content with some content that should be encrypted properly.";
    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_PASSWORD = "secure-password-123";
    private static final String TEST_PEPPER = "test-application-pepper-for-key-derivation-security";

    @BeforeEach
    void setUp() {
        // Set up test key for encryption
        System.setProperty("knotes.encryption.key", TEST_PEPPER);
    }

    @Test
    @DisplayName("Should encrypt and decrypt data successfully with round-trip integrity")
    void testEncryptDecryptRoundTrip() {
        // Given
        byte[] originalData = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] key = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);

        // When
        byte[] encrypted = EncryptionUtils.encrypt(originalData, key);
        byte[] decrypted = EncryptionUtils.decrypt(encrypted, key, salt);

        // Then
        assertNotNull(encrypted);
        assertNotNull(decrypted);
        assertArrayEquals(originalData, decrypted);

        // Verify encrypted data is different from original
        assertNotEquals(originalData.length, encrypted.length);
        assertFalse(java.util.Arrays.equals(originalData, encrypted));
    }

    @Test
    @DisplayName("Should generate unique salts for each invocation")
    void testSaltUniqueness() {
        // When
        byte[] salt1 = EncryptionUtils.generateSalt();
        byte[] salt2 = EncryptionUtils.generateSalt();
        byte[] salt3 = EncryptionUtils.generateSalt();

        // Then
        assertEquals(16, salt1.length); // Verify correct salt length
        assertEquals(16, salt2.length);
        assertEquals(16, salt3.length);

        assertFalse(java.util.Arrays.equals(salt1, salt2));
        assertFalse(java.util.Arrays.equals(salt2, salt3));
        assertFalse(java.util.Arrays.equals(salt1, salt3));
    }

    @Test
    @DisplayName("Should derive same owner key for same user ID and salt")
    void testOwnerKeyDeterministic() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When
        byte[] key1 = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);
        byte[] key2 = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);

        // Then
        assertNotNull(key1);
        assertNotNull(key2);
        assertEquals(32, key1.length); // 256 bits = 32 bytes
        assertArrayEquals(key1, key2);
    }

    @Test
    @DisplayName("Should derive different keys for different user IDs")
    void testOwnerKeyUserSpecific() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When
        byte[] key1 = EncryptionUtils.deriveOwnerKey("user1", salt);
        byte[] key2 = EncryptionUtils.deriveOwnerKey("user2", salt);

        // Then
        assertNotNull(key1);
        assertNotNull(key2);
        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    @DisplayName("Should derive same password key for same password and salt")
    void testPasswordKeyDeterministic() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When
        byte[] key1 = EncryptionUtils.derivePasswordKey(TEST_PASSWORD, salt);
        byte[] key2 = EncryptionUtils.derivePasswordKey(TEST_PASSWORD, salt);

        // Then
        assertNotNull(key1);
        assertNotNull(key2);
        assertEquals(32, key1.length); // 256 bits = 32 bytes
        assertArrayEquals(key1, key2);
    }

    @Test
    @DisplayName("Should derive different keys for different passwords")
    void testPasswordKeyPasswordSpecific() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When
        byte[] key1 = EncryptionUtils.derivePasswordKey("password1", salt);
        byte[] key2 = EncryptionUtils.derivePasswordKey("password2", salt);

        // Then
        assertNotNull(key1);
        assertNotNull(key2);
        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    @DisplayName("Should fail decryption with wrong key")
    void testDecryptWithWrongKeyFails() {
        // Given
        byte[] originalData = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] correctKey = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);
        byte[] wrongKey = EncryptionUtils.deriveOwnerKey("different-user", salt);

        byte[] encrypted = EncryptionUtils.encrypt(originalData, correctKey);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EncryptionUtils.decrypt(encrypted, wrongKey, salt);
        });

        assertTrue(exception.getMessage().contains("Failed to decrypt data"));
    }

    @Test
    @DisplayName("Should fail decryption with wrong password-derived key")
    void testDecryptWithWrongPasswordFails() {
        // Given
        byte[] originalData = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] correctKey = EncryptionUtils.derivePasswordKey(TEST_PASSWORD, salt);
        byte[] wrongKey = EncryptionUtils.derivePasswordKey("wrong-password", salt);

        byte[] encrypted = EncryptionUtils.encrypt(originalData, correctKey);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EncryptionUtils.decrypt(encrypted, wrongKey, salt);
        });

        assertTrue(exception.getMessage().contains("Failed to decrypt data"));
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void testEncryptEmptyData() {
        // Given
        byte[] emptyData = new byte[0];
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] key = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);

        // When
        byte[] encrypted = EncryptionUtils.encrypt(emptyData, key);
        byte[] decrypted = EncryptionUtils.decrypt(encrypted, key, salt);

        // Then
        assertNotNull(encrypted);
        assertNotNull(decrypted);
        assertEquals(0, decrypted.length);
        assertArrayEquals(emptyData, decrypted);
    }

    @Test
    @DisplayName("Should handle null data gracefully")
    void testEncryptNullData() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] key = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);

        // When
        byte[] encrypted = EncryptionUtils.encrypt(null, key);

        // Then
        assertNotNull(encrypted);
        assertEquals(0, encrypted.length);
    }

    @Test
    @DisplayName("Should validate key length requirements")
    void testInvalidKeyLength() {
        // Given
        byte[] data = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] shortKey = new byte[16]; // 128 bits instead of required 256

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.encrypt(data, shortKey);
        });

        assertTrue(exception.getMessage().contains("Key must be exactly 256 bits"));
    }

    @Test
    @DisplayName("Should validate salt length for owner key derivation")
    void testInvalidSaltLengthOwner() {
        // Given
        byte[] shortSalt = new byte[8]; // Too short

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.deriveOwnerKey(TEST_USER_ID, shortSalt);
        });

        assertTrue(exception.getMessage().contains("Salt must be exactly 16 bytes"));
    }

    @Test
    @DisplayName("Should validate salt length for password key derivation")
    void testInvalidSaltLengthPassword() {
        // Given
        byte[] shortSalt = new byte[8]; // Too short

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.derivePasswordKey(TEST_PASSWORD, shortSalt);
        });

        assertTrue(exception.getMessage().contains("Salt must be exactly 16 bytes"));
    }

    @Test
    @DisplayName("Should fail key derivation with null user ID")
    void testNullUserId() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.deriveOwnerKey(null, salt);
        });

        assertTrue(exception.getMessage().contains("User ID cannot be null or empty"));
    }

    @Test
    @DisplayName("Should fail key derivation with empty user ID")
    void testEmptyUserId() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.deriveOwnerKey("", salt);
        });

        assertTrue(exception.getMessage().contains("User ID cannot be null or empty"));
    }

    @Test
    @DisplayName("Should fail key derivation with null password")
    void testNullPassword() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.derivePasswordKey(null, salt);
        });

        assertTrue(exception.getMessage().contains("Password cannot be null or empty"));
    }

    @Test
    @DisplayName("Should fail key derivation with empty password")
    void testEmptyPassword() {
        // Given
        byte[] salt = EncryptionUtils.generateSalt();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            EncryptionUtils.derivePasswordKey("", salt);
        });

        assertTrue(exception.getMessage().contains("Password cannot be null or empty"));
    }

    @Test
    @DisplayName("Should fail decryption with corrupted data")
    void testDecryptCorruptedData() {
        // Given
        byte[] originalData = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] key = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);

        byte[] encrypted = EncryptionUtils.encrypt(originalData, key);

        // Corrupt the encrypted data
        encrypted[encrypted.length - 1] = (byte) (encrypted[encrypted.length - 1] ^ 0xFF);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EncryptionUtils.decrypt(encrypted, key, salt);
        });

        assertTrue(exception.getMessage().contains("Failed to decrypt data"));
    }

    @Test
    @DisplayName("Should fail decryption with invalid encrypted data format")
    void testDecryptInvalidFormat() {
        // Given
        byte[] invalidData = "not-encrypted-data".getBytes(StandardCharsets.UTF_8);
        byte[] salt = EncryptionUtils.generateSalt();
        byte[] key = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EncryptionUtils.decrypt(invalidData, key, salt);
        });

        assertTrue(exception.getMessage().contains("Invalid encrypted data format") ||
                   exception.getMessage().contains("Failed to decrypt data"));
    }

    @Test
    @DisplayName("Should calculate encryption overhead correctly")
    void testEncryptionOverhead() {
        // Given
        int originalSize = 1000;
        int encryptedSize = 1100;

        // When
        double overhead = EncryptionUtils.getEncryptionOverhead(originalSize, encryptedSize);

        // Then
        assertEquals(0.1, overhead, 0.001); // 10% overhead
    }

    @Test
    @DisplayName("Should handle zero original size in overhead calculation")
    void testEncryptionOverheadZeroOriginal() {
        // Given
        int originalSize = 0;
        int encryptedSize = 100;

        // When
        double overhead = EncryptionUtils.getEncryptionOverhead(originalSize, encryptedSize);

        // Then
        assertEquals(0.0, overhead, 0.001);
    }

    @Test
    @DisplayName("Should validate configuration successfully with pepper")
    void testValidateConfigurationSuccess() {
        // Given - pepper is already set in setUp()

        // When & Then - should not throw
        assertDoesNotThrow(EncryptionUtils::validateConfiguration);
    }

    @Test
    @DisplayName("Should fail validation when encryption key is not configured")
    void testValidateConfigurationMissingKey() {
        // Given
        System.clearProperty("knotes.encryption.key");

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            EncryptionUtils.validateConfiguration();
        });

        assertTrue(exception.getMessage().contains("encryption_key environment variable must be configured"));
    }

    @Test
    @DisplayName("Should fail validation when encryption key is too short")
    void testValidateConfigurationShortKey() {
        // Given
        System.setProperty("knotes.encryption.key", "short");

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            EncryptionUtils.validateConfiguration();
        });

        assertTrue(exception.getMessage().contains("encryption_key must be at least 32 characters long"));
    }

    @Test
    @DisplayName("Should produce different ciphertext with same content but different salts")
    void testDifferentSaltsProduceDifferentCiphertext() {
        // Given
        byte[] content = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] salt1 = EncryptionUtils.generateSalt();
        byte[] salt2 = EncryptionUtils.generateSalt();

        byte[] key1 = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt1);
        byte[] key2 = EncryptionUtils.deriveOwnerKey(TEST_USER_ID, salt2);

        // When
        byte[] encrypted1 = EncryptionUtils.encrypt(content, key1);
        byte[] encrypted2 = EncryptionUtils.encrypt(content, key2);

        // Then
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2));

        // But both should decrypt to the same content
        byte[] decrypted1 = EncryptionUtils.decrypt(encrypted1, key1, salt1);
        byte[] decrypted2 = EncryptionUtils.decrypt(encrypted2, key2, salt2);

        assertArrayEquals(content, decrypted1);
        assertArrayEquals(content, decrypted2);
    }
}