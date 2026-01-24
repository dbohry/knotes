package com.lhamacorp.knotes.api.dto;

import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;

import java.time.Instant;

/**
 * Metadata-only response DTO for content information without content.
 *
 * <p>Provides lightweight content information including encryption status
 * without the overhead of decrypting content. Useful for list views
 * and metadata operations.</p>
 */
public record NoteMetadata(
        String id,
        Instant createdAt,
        Instant modifiedAt,
        EncryptionMode encryptionMode,
        Boolean requiresPassword
) {

    /**
     * Creates metadata from a content without retrieving content.
     *
     * @param note the content to extract metadata from
     * @return NoteMetadata with encryption status information
     */
    public static NoteMetadata from(Note note) {
        return new NoteMetadata(
            note.id(),
            note.createdAt(),
            note.modifiedAt(),
            note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
            note.requiresPassword() != null ? note.requiresPassword() : false
        );
    }
}