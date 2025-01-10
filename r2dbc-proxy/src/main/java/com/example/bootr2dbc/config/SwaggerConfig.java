package com.example.bootr2dbc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(title = "boot-r2dbc-sample", version = "v1"),
        servers = @Server(url = "/"),
        security = @SecurityRequirement(name = "Authorization"))
@SecurityScheme(type = SecuritySchemeType.HTTP, scheme = "basic", name = "Authorization")
public class SwaggerConfig {}
