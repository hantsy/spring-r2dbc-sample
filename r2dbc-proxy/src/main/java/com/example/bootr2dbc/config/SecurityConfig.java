package com.example.bootr2dbc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        var postPath = "/api/posts/**";
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(
                        it ->
                                it.pathMatchers(
                                                "/webjars/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs/**")
                                        .permitAll() // Allow Springdoc OpenAPI resources
                                        .pathMatchers(HttpMethod.DELETE, postPath)
                                        .hasRole("ADMIN")
                                        .pathMatchers(postPath)
                                        .hasRole("USER")
                                        .pathMatchers("/users/{user}/**")
                                        .access(this::currentUserMatchesPath)
                                        .anyExchange()
                                        .authenticated())
                .build();
    }

    private Mono<AuthorizationDecision> currentUserMatchesPath(
            Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .map(a -> context.getVariables().get("user").equals(a.getName()))
                .map(AuthorizationDecision::new);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean(name = "userDetailsService")
    MapReactiveUserDetailsService userDetailsServiceWithPasswordEncoder(
            PasswordEncoder passwordEncoder) {
        UserDetails user =
                User.withUsername("user")
                        .passwordEncoder(passwordEncoder::encode)
                        .password("password")
                        .roles("USER")
                        .build();
        UserDetails admin =
                User.withUsername("admin")
                        .passwordEncoder(passwordEncoder::encode)
                        .password("password")
                        .roles("USER", "ADMIN")
                        .build();
        return new MapReactiveUserDetailsService(user, admin);
    }
}
