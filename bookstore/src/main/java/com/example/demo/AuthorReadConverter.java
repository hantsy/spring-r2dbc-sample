package com.example.demo;

import io.r2dbc.spi.Row;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.UUID;

@ReadingConverter
public class AuthorReadConverter implements Converter<Row, Author> {
    @Override
    public Author convert(Row row) {
        return new Author(
                row.get("id", UUID.class),
                row.get("first_name", String.class),
                row.get("last_name", String.class),
                new Address(
                        row.get("address_street", String.class),
                        row.get("zipcode", String.class),
                        row.get("city", String.class)
                )
        );
    }
}
