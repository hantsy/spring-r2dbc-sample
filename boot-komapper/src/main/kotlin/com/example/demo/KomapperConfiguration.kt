package com.example.demo

import io.r2dbc.spi.ConnectionFactory
import org.komapper.dialect.postgresql.r2dbc.PostgreSqlR2dbcDialect
import org.komapper.r2dbc.DefaultR2dbcDatabaseConfig
import org.komapper.r2dbc.R2dbcDatabase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class KomapperConfiguration {

    @Bean
    fun r2dbcDatabase(connectionFactory: ConnectionFactory) =
        R2dbcDatabase(
            DefaultR2dbcDatabaseConfig(
                connectionFactory = connectionFactory,
                dialect = PostgreSqlR2dbcDialect()
            )
        )
}