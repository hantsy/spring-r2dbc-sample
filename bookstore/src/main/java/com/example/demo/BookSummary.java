package com.example.demo;

import lombok.Value;

import java.util.UUID;

@Value
class BookSummary {
    UUID id;
    String title;
}
