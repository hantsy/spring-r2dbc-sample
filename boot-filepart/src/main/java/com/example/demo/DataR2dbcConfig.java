package com.example.demo;


import io.r2dbc.spi.Blob;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;

@Configuration
public class DataR2dbcConfig {

    // see: https://github.com/spring-projects/spring-data-relational/issues/1408
    // fixed in spring data relational 3.1
//    @Bean
//    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
//        return R2dbcCustomConversions.of(
//                DialectResolver.getDialect(connectionFactory),
//                List.of(
//                        new ByteArrayToByteBufferConverter(),
//                        new ByteBufferToByteArrayConverter(),
//                        new ByteArrayToBlobConverter(),
//                        new BlobToByteArrayConverter()
//                )
//        );
//    }
}


@ReadingConverter
class ByteArrayToByteBufferConverter implements Converter<byte[], ByteBuffer> {

    @Override
    public ByteBuffer convert(byte[] source) {
        return ByteBuffer.wrap(source);
    }
}

@WritingConverter
class ByteBufferToByteArrayConverter implements Converter<ByteBuffer, byte[]> {

    @Override
    public byte[] convert(ByteBuffer source) {
        return source.array();
    }
}

@ReadingConverter
class ByteArrayToBlobConverter implements Converter<byte[], Blob> {

    @Override
    public Blob convert(byte[] source) {
        return Blob.from(Mono.just(ByteBuffer.wrap(source)));
    }
}

@WritingConverter
class BlobToByteArrayConverter implements Converter<Blob, byte[]> {

    @Override
    public byte[] convert(Blob source) {
        return Mono.from(source.stream()).block().array();
    }
}
