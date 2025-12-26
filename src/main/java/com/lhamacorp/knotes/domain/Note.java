package com.lhamacorp.knotes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("notes")
public record Note(@Id String id, String content, Instant createdAt, Instant modifiedAt) {
}
