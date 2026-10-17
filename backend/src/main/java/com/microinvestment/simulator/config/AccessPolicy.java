package com.microinvestment.simulator.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AccessPolicy {
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new AccessDeniedException("Authentication is required.");
        }
        Object userId = jwt.getToken().getClaim("userId");
        if (!(userId instanceof Number) || !userId.toString().matches("[1-9][0-9]*")) {
            throw new AccessDeniedException("Invalid account identity.");
        }
        try {
            return Long.parseLong(userId.toString());
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid account identity.");
        }
    }

    public void requireOwner(Long userId) {
        if (!currentUserId().equals(userId)) {
            throw new AccessDeniedException("You can only access your own account.");
        }
    }
}
