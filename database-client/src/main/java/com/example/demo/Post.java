package com.example.demo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.r2dbc.postgresql.codec.Json;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author hantsy
 */
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    private UUID id;

    private String title;

    private String content;

    @JsonSerialize(using = PgJsonObjectSerializer.class)
    @JsonDeserialize(using = PgJsonObjectDeserializer.class)
    @Builder.Default
    private Json metadata = Json.of("{}");

    @Builder.Default
    private Status status = Status.DRAFT;

    private LocalDateTime createdAt;

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}
