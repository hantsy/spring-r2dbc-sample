package com.example.demo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class PgJsonObjectDeserializer extends JsonDeserializer<Json> {

    @Override
    public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        var value = ctxt.readTree(p);
        log.info("read json value : {}", value);
        return Json.of(value.toString());
    }
}
