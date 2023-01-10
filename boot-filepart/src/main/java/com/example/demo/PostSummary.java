package com.example.demo;

import lombok.Value;

import java.util.UUID;

public record  PostSummary (
        UUID id,
        String title
){}

