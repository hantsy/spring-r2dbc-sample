package com.example.demo;

import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Blob;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "posts")
class Post {

    @Id
    @Column("id")
    private UUID id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("metadata")
    private Json metadata;

    @Column("status")
    private Status status;

    @Column("attachment")
    private ByteBuffer attachment;

    @Column("cover_image")
    private byte[] coverImage;

    @Column("cover_image_thumbnail")
    private Blob coverImageThumbnail;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("version")
    @Version
    private Long version;

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}
