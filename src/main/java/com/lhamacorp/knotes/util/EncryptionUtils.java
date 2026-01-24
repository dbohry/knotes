package com.lhamacorp.knotes.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

public class EncryptionUtils {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int IV_LENGTH = 12; // 96 bits for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // 128 bits
    private static final int PBKDF2_ITERATIONS = 1_000;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static byte[] encrypt(byte[] data, byte[] key) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        if (key == null || key.length != KEY_LENGTH / 8) {
            throw new IllegalArgumentException("Key must be exactly 256 bits (32 bytes)");
        }

        try {
            // Generate random IV for this encryption
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt the data
            byte[] ciphertext = cipher.doFinal(data);

            // Format: [IV][Ciphertext with embedded auth tag]
            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedData, byte[] key, byte[] salt) {
        if (encryptedData == null || encryptedData.length == 0) {
            return new byte[0];
        }

        if (encryptedData.length < IV_LENGTH + GCM_TAG_LENGTH) {
            throw new RuntimeException("Invalid encrypted data format");
        }

        if (key == null || key.length != KEY_LENGTH / 8) {
            throw new IllegalArgumentException("Key must be exactly 256 bits (32 bytes)");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

            // Extract IV
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            // Extract ciphertext (includes auth tag)
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt and verify
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    public static byte[] deriveOwnerKey(String userId, byte[] salt) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        if (salt == null || salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("Salt must be exactly " + SALT_LENGTH + " bytes");
        }

        // Get application key from environment
        String key = System.getProperty("knotes.encryption.key");
        if (key == null) {
            key = System.getenv("encryption_key");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("encryption_key environment variable not configured");
        }

        // Combine user ID with application key
        String keyMaterial = userId + key;

        return deriveKey(keyMaterial, salt);
    }

    public static byte[] derivePasswordKey(String password, byte[] salt) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (salt == null || salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("Salt must be exactly " + SALT_LENGTH + " bytes");
        }

        return deriveKey(password, salt);
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    private static byte[] deriveKey(String keyMaterial, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    keyMaterial.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();

            spec.clearPassword();

            return derivedKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    public static double getEncryptionOverhead(int originalSize, int encryptedSize) {
        if (originalSize == 0) return 0.0;
        return ((double) encryptedSize - originalSize) / originalSize;
    }

    public static void validateConfiguration() {
        String key = System.getProperty("knotes.encryption.key");
        if (key == null) {
            key = System.getenv("encryption_key");
        }

        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("encryption_key environment variable must be configured");
        }

        if (key.length() < 32) {
            throw new IllegalStateException("encryption_key must be at least 32 characters long");
        }
    }
}