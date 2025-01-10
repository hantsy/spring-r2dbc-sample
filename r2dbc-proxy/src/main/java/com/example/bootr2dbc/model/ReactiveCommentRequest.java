package com.example.bootr2dbc.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ReactiveCommentRequest(
        @NotBlank(message = "Title must not be blank") String title,
        @NotBlank(message = "Content must not be blank") String content,
        @Positive(message = "PostId must be greater than 0") Long postId,
        boolean published) {}
