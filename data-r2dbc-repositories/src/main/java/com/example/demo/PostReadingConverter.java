package com.example.demo;

import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Row;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.LocalDateTime;
import java.util.UUID;

@ReadingConverter
public class PostReadingConverter implements Converter<Row, Post> {

    @Override
    public Post convert(Row row) {
        return Post.builder()
                .id(row.get("id", UUID.class))
                .title(row.get("title", String.class))
                .content(row.get("content", String.class))
                .status(row.get("status", Post.Status.class))
                .metadata(row.get("metadata", Json.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .build();
    }
}
