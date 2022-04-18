package com.example.demo;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.r2dbc.core.Parameter;

import java.util.Map;

public class AuthorWriteConverter implements Converter<Author, OutboundRow> {
    @Override
    public OutboundRow convert(Author source) {
        var row = new OutboundRow();
        Map.of(
                        "first_name", Parameter.fromOrEmpty(source.firstName(), String.class),
                        "last_name", Parameter.fromOrEmpty(source.lastName(), String.class),
                        "address_street", Parameter.fromOrEmpty(source.address().street(), String.class),
                        "zipcode", Parameter.fromOrEmpty(source.address().zipCode(), String.class),
                        "city", Parameter.fromOrEmpty(source.address().city(), String.class)
                )
                .forEach(row::put);

        return row;
    }
}
