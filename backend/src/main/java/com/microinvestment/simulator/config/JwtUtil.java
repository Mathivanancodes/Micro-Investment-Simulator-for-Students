package com.microinvestment.simulator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@Slf4j
public class JwtUtil {

    private static final String SECRET_KEY = "apextradesecretkeywhichisextremelylongtobesecure";
    private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    /**
     * Generates a JWT token for the user. Expires in 7 days.
     */
    public String generateToken(String username, Long userId, String role) {
        long exp = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000); // 7 days
        
        // Simple manual JSON construction to avoid library dependencies
        String payload = String.format(
                "{\"sub\":\"%s\",\"userId\":%d,\"role\":\"%s\",\"exp\":%d}",
                username, userId, role, exp
        );
        
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        
        String data = HEADER + "." + payloadBase64;
        String signature = sign(data, SECRET_KEY);
        
        return data + "." + signature;
    }

    /**
     * Validates a JWT token's signature and expiration date.
     */
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            
            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];
            
            // 1. Verify Signature
            String expectedSignature = sign(header + "." + payload, SECRET_KEY);
            if (!expectedSignature.equals(signature)) {
                log.warn("Invalid JWT Signature detected.");
                return false;
            }
            
            // 2. Verify Expiration
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            long exp = extractExpClaim(decodedPayload);
            if (System.currentTimeMillis() > exp) {
                log.warn("JWT Token has expired.");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username (sub claim) from token payload.
     */
    public String getUsernameFromToken(String token) {
        String payload = decodePayload(token);
        return extractStringClaim(payload, "sub");
    }

    /**
     * Extract user ID (userId claim) from token payload.
     */
    public Long getUserIdFromToken(String token) {
        String payload = decodePayload(token);
        return extractLongClaim(payload, "userId");
    }

    /**
     * Extract user role (role claim) from token payload.
     */
    public String getRoleFromToken(String token) {
        String payload = decodePayload(token);
        return extractStringClaim(payload, "role");
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error signing JWT token", e);
        }
    }

    private long extractExpClaim(String json) {
        String token = "\"exp\":";
        int idx = json.indexOf(token);
        if (idx == -1) return 0;
        int start = idx + token.length();
        int end = json.indexOf("}", start);
        if (end == -1) end = json.indexOf(",", start);
        return Long.parseLong(json.substring(start, end).trim());
    }

    private String extractStringClaim(String json, String claim) {
        String token = "\"" + claim + "\":\"";
        int idx = json.indexOf(token);
        if (idx == -1) return null;
        int start = idx + token.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private Long extractLongClaim(String json, String claim) {
        String token = "\"" + claim + "\":";
        int idx = json.indexOf(token);
        if (idx == -1) return null;
        int start = idx + token.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}
