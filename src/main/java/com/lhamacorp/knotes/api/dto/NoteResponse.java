package com.lhamacorp.knotes.api.dto;

import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;

import java.time.Instant;

public record NoteResponse(
        String id,
        String content,
        Instant createdAt,
        Instant modifiedAt,
        EncryptionMode encryptionMode,
        Boolean requiresPassword
) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.id(),
                note.content(),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false
        );
    }

    public static NoteResponse fromPrivate(Note note, String requestingUserId) {
        return new NoteResponse(
                note.id(),
                note.content(requestingUserId, null),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false
        );
    }

    public static NoteResponse fromPasswordShared(Note note, String password) {
        return new NoteResponse(
                note.id(),
                note.content(null, password),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false
        );
    }

    public static NoteResponse fromWithAuth(Note note, String requestingUserId, String password) {
        return new NoteResponse(
                note.id(),
                note.content(requestingUserId, password),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false
        );
    }
}