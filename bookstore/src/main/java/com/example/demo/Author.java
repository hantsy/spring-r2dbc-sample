package com.example.demo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("authors")
public record Author(@Id
                     @Column("id")
                     UUID id,
                     @Column("first_name")
                     String firstName,
                     @Column("last_name")
                     String lastName,
                     @Embedded.Empty
                     Address address
) {
}
