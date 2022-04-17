package com.example.demo;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "reviews")
class Review {

    @Id
    @Column("id")
    private UUID id;

    @Column("content")
    private String content;

    @Column("status")
    private Status status = Status.DRAFT;

    @Column("book_id")
    private UUID bookId;

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
