package com.microinvestment.simulator.config;

import com.microinvestment.simulator.dto.AuthRequest;
import com.microinvestment.simulator.dto.AuthResponse;
import com.microinvestment.simulator.dto.RegisterRequest;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.exception.BusinessRuleException;
import com.microinvestment.simulator.repository.UserRepository;
import com.microinvestment.simulator.repository.WalletRepository;
import com.microinvestment.simulator.service.AuthService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityBoundaryTest {
    private final SecurityConfig security = new SecurityConfig();
    private final JwtEncoder encoder = security.jwtEncoder(security.jwtKey("test-only-secret-of-at-least-thirty-two-bytes"));
    private final JwtDecoder decoder = security.jwtDecoder(security.jwtKey("test-only-secret-of-at-least-thirty-two-bytes"));
    private final JwtService jwt = new JwtService(encoder);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtSecretRequiresAtLeastThirtyTwoUtf8Bytes() {
        assertThatThrownBy(() -> security.jwtKey("short-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 UTF-8 bytes");
        assertThatCode(() -> security.jwtKey("é".repeat(16))).doesNotThrowAnyException();
    }

    @Test
    void issuedTokenContainsVerifiedIdentityAndExpiresAfterEightHoursWithoutPasswordClaims() {
        User user = user("Student Name", "student@example.test", "never-expose-this-hash");

        Jwt decoded = decoder.decode(jwt.generateToken(user));

        assertThat(decoded.getSubject()).isEqualTo("student@example.test");
        assertThat(((Number) decoded.getClaim("userId")).longValue()).isEqualTo(42L);
        assertThat(decoded.getClaimAsString("name")).isEqualTo("Student Name");
        assertThat(decoded.getExpiresAt()).isEqualTo(decoded.getIssuedAt().plusSeconds(28800));
        assertThat(decoded.getClaims()).doesNotContainKeys("password", "passwordHash", "credentials");
    }

    @Test
    void tamperedAndDifferentlySignedTokensAreRejected() {
        String token = jwt.generateToken(user("Student", "student@example.test", "hash"));
        String[] segments = token.split("\\.");
        segments[2] = (segments[2].startsWith("A") ? "B" : "A") + segments[2].substring(1);
        assertThatThrownBy(() -> decoder.decode(String.join(".", segments))).isInstanceOf(JwtException.class);

        JwtEncoder foreignEncoder = security.jwtEncoder(security.jwtKey("another-test-only-secret-of-thirty-two-bytes"));
        String foreign = encode(foreignEncoder, claims(JwtService.ISSUER, Instant.now().plusSeconds(300)));
        assertThatThrownBy(() -> decoder.decode(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void wrongIssuerExpiredAndNotYetValidTokensAreRejected() {
        String wrongIssuer = encode(encoder, claims("different-application", Instant.now().plusSeconds(300)));
        String expired = encode(encoder, claims(JwtService.ISSUER, Instant.now().minusSeconds(300)));
        String future = encode(encoder, JwtClaimsSet.builder()
                .issuer(JwtService.ISSUER).subject("student@example.test").claim("userId", 42L)
                .issuedAt(Instant.now()).notBefore(Instant.now().plusSeconds(300))
                .expiresAt(Instant.now().plusSeconds(600)).build());

        assertThatThrownBy(() -> decoder.decode(wrongIssuer)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(future)).isInstanceOf(JwtException.class);
    }

    @Test
    void signedTokensWithMissingOrMalformedRequiredIdentityClaimsAreRejected() {
        for (Object invalid : new Object[]{"42", 42.5, 0L, -1L, new java.math.BigInteger("9223372036854775808")}) {
            JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtService.ISSUER)
                    .subject("student@example.test").expiresAt(Instant.now().plusSeconds(300))
                    .claim("userId", invalid).build();
            assertThatThrownBy(() -> decoder.decode(encode(encoder, claims))).isInstanceOf(JwtException.class);
        }
        JwtClaimsSet missingExpiry = JwtClaimsSet.builder().issuer(JwtService.ISSUER)
                .subject("student@example.test").claim("userId", 42L).build();
        JwtClaimsSet missingIdentity = JwtClaimsSet.builder().issuer(JwtService.ISSUER)
                .subject("student@example.test").expiresAt(Instant.now().plusSeconds(300)).build();
        JwtClaimsSet missingSubject = JwtClaimsSet.builder().issuer(JwtService.ISSUER)
                .claim("userId", 42L).expiresAt(Instant.now().plusSeconds(300)).build();
        for (JwtClaimsSet malformed : new JwtClaimsSet[]{missingExpiry, missingIdentity, missingSubject}) {
            assertThatThrownBy(() -> decoder.decode(encode(encoder, malformed))).isInstanceOf(JwtException.class);
        }
    }

    @Test
    void ownerPolicyRejectsAnonymousNonJwtAndOtherUsers() {
        AccessPolicy access = new AccessPolicy();
        assertThatThrownBy(access::currentUserId).isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student", "redacted"));
        assertThatThrownBy(access::currentUserId).isInstanceOf(AccessDeniedException.class);

        authenticate(Jwt.withTokenValue("verified-test-token").header("alg", "HS256").claim("userId", 42L).build());
        assertThatCode(() -> access.requireOwner(42L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireOwner(43L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingMalformedNonIntegralAndNonPositiveIdentityAreDeniedSafely() {
        AccessPolicy access = new AccessPolicy();
        for (Object invalid : new Object[]{"42", 42.5, 0L, -1L}) {
            authenticate(Jwt.withTokenValue("verified-test-token").header("alg", "HS256").claim("userId", invalid).build());
            assertThatThrownBy(access::currentUserId).isInstanceOf(AccessDeniedException.class);
        }
        authenticate(Jwt.withTokenValue("verified-test-token").header("alg", "HS256").claim("name", "Student").build());
        assertThatThrownBy(access::currentUserId).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void registrationPreservesUnicodeNameAndNormalizesEmailWithoutExposingHash() {
        UserRepository users = mock(UserRepository.class);
        WalletRepository wallets = mock(WalletRepository.class);
        PasswordEncoder passwords = security.passwordEncoder();
        AuthService auth = new AuthService(users, wallets, passwords, jwt);
        RegisterRequest request = new RegisterRequest("மதி மாணவர்", "STUDENT+தேர்வு@EXAMPLE.TEST", "é".repeat(36));
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(request)).isEmpty();
        }
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            assertThat(saved.getName()).isEqualTo("மதி மாணவர்");
            assertThat(saved.getEmail()).isEqualTo("student+தேர்வு@example.test");
            assertThat(saved.getPasswordHash()).doesNotContain(request.password());
            assertThat(passwords.matches(request.password(), saved.getPasswordHash())).isTrue();
            return saved;
        });

        AuthResponse response = auth.register(request);

        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("மதி மாணவர்");
        assertThat(response.email()).isEqualTo("student+தேர்வு@example.test");
        assertThat(decoder.decode(response.token()).getSubject()).isEqualTo(response.email());
    }

    @Test
    void multibyteRegistrationBeyondBcryptLimitDoesNotReachPersistence() {
        UserRepository users = mock(UserRepository.class);
        WalletRepository wallets = mock(WalletRepository.class);
        AuthService auth = new AuthService(users, wallets, security.passwordEncoder(), jwt);

        assertThatThrownBy(() -> auth.register(new RegisterRequest("Student", "student@example.test", "é".repeat(37))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("72 UTF-8 bytes");
        verify(users, never()).saveAndFlush(any());
        verify(wallets, never()).saveAndFlush(any());
    }

    @Test
    void multibyteLoginBeyondBcryptLimitReturnsCredentialFailure() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = security.passwordEncoder();
        User user = user("Student", "student@example.test", passwords.encode("é".repeat(36)));
        when(users.findByEmail("student@example.test")).thenReturn(Optional.of(user));
        AuthService auth = new AuthService(users, mock(WalletRepository.class), passwords, jwt);

        assertThatThrownBy(() -> auth.login(new AuthRequest("student@example.test", "é".repeat(37))))
                .isInstanceOf(BadCredentialsException.class);
    }

    private static User user(String name, String email, String passwordHash) {
        User user = new User(name, email, passwordHash);
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    private static void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static JwtClaimsSet claims(String issuer, Instant expiry) {
        return JwtClaimsSet.builder().issuer(issuer).subject("student@example.test").claim("userId", 42L)
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiry).build();
    }

    private static String encode(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
