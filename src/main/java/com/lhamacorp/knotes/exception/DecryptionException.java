package com.lhamacorp.knotes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when content decryption fails.
 *
 * <p>This exception is thrown in the following scenarios:</p>
 * <ul>
 *   <li>Incorrect password provided for PASSWORD_SHARED notes</li>
 *   <li>Corrupted or tampered encryption data</li>
 *   <li>Missing password when accessing PASSWORD_SHARED notes</li>
 *   <li>Invalid encryption format or metadata</li>
 * </ul>
 *
 * <p>Returns HTTP 400 Bad Request to prevent information leakage about
 * the specific cause of decryption failure (security by obscurity).</p>
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DecryptionException extends RuntimeException {

    /**
     * Constructs a new DecryptionException with the specified detail message.
     *
     * @param message the detail message explaining the decryption failure
     */
    public DecryptionException(String message) {
        super(message);
    }

    /**
     * Constructs a new DecryptionException with the specified detail message and cause.
     *
     * @param message the detail message explaining the decryption failure
     * @param cause the underlying cause of the decryption failure
     */
    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}