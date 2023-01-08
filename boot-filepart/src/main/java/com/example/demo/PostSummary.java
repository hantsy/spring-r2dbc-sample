package com.example.demo;

import lombok.Value;

import java.util.UUID;

@Value
class PostSummary {
    UUID id;
    String title;
}
