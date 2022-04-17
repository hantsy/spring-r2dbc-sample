package com.example.demo;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "books")
class Book {

    @Id
    @Column("id")
    private UUID id;

    @Column("isbn")
    private String isbn;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("tags")
    private List<String> tags = Collections.emptyList();

    // @MappedCollection(idColumn = "book_id", keyColumn = "key")
    @Column("metadata")
    private Map<String, String> metadata = Collections.emptyMap();

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("version")
    @Version
    private Long version;
}
