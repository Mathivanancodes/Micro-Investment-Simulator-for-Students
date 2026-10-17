package com.microinvestment.simulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microinvestment.simulator.dto.ErrorResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtKey(@Value("${app.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 UTF-8 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> identity = token -> {
            Object id = token.getClaims().get("userId");
            boolean valid = token.getExpiresAt() != null && token.getSubject() != null
                    && !token.getSubject().isBlank() && id instanceof Number
                    && id.toString().matches("[1-9][0-9]*");
            try {
                valid = valid && Long.parseLong(id.toString()) > 0;
            } catch (RuntimeException exception) {
                valid = false;
            }
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token identity or expiry is invalid.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.ISSUER), identity));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stocks", "/api/stocks/**", "/api/leaderboard").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(jwt -> {})
                        .authenticationEntryPoint((request, response, failure) ->
                                writeError(mapper, request, response, 401, "Unauthorized", "A valid bearer token is required.")))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, failure) ->
                                writeError(mapper, request, response, 401, "Unauthorized", "Authentication is required."))
                        .accessDeniedHandler((request, response, failure) ->
                                writeError(mapper, request, response, 403, "Forbidden", "Access denied.")));
        return http.build();
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                   int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(),
                new ErrorResponse(LocalDateTime.now(), status, error, message, request.getRequestURI()));
    }
}
