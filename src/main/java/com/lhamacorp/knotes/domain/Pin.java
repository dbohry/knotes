package com.lhamacorp.knotes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("pins")
public record Pin(@Id String id, String noteId, String userId, Instant createdAt) {
}
