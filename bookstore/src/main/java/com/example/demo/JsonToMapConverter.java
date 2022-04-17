package com.example.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ReadingConverter
@RequiredArgsConstructor
@Slf4j
public class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
    private final ObjectMapper objectMapper;
    @Override
    public Map<String, Object> convert(Json json) {
        try {
            return objectMapper.readValue(json.asString(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Problem while parsing JSON: {}", json, e);
        }
        return new HashMap<>();
    }
}
