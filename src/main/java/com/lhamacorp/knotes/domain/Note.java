package com.lhamacorp.knotes.domain;

import com.lhamacorp.knotes.util.CompressionUtils;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document("notes")
public record Note(
    @Id String id,
    @Field("content") Binary compressedData,
    Instant createdAt,
    Instant modifiedAt
) {

    public Note(String id, String content, Instant createdAt, Instant modifiedAt) {
        this(id, content != null ? new Binary(CompressionUtils.compress(content)) : null, createdAt, modifiedAt);
    }

    public String content() {
        if (compressedData == null) {
            return null;
        }
        return CompressionUtils.decompress(compressedData.getData());
    }
}
