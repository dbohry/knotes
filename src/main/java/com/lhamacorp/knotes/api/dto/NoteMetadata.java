package com.lhamacorp.knotes.api.dto;

import com.lhamacorp.knotes.domain.Note;

import java.time.Instant;

public record NoteMetadata(String id, Instant createdAt, Instant modifiedAt) {

    public static NoteMetadata from(Note note) {
        return new NoteMetadata(
            note.id(),
            note.createdAt(),
            note.modifiedAt()
        );
    }
}