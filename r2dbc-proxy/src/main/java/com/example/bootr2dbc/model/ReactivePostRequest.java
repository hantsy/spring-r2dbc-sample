package com.example.bootr2dbc.model;

import jakarta.validation.constraints.NotBlank;

public record ReactivePostRequest(
        @NotBlank(message = "Title must not be blank") String title,
        @NotBlank(message = "Content must not be blank") String content) {}
