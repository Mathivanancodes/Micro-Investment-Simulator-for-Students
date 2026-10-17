package com.microinvestment.simulator.config;

import com.microinvestment.simulator.entity.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final String ISSUER = "micro-investment-simulator";
    private final JwtEncoder encoder;

    public JwtService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject(user.getEmail())
                .issuedAt(now).expiresAt(now.plusSeconds(28800)).claim("userId", user.getId())
                .claim("name", user.getName()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
