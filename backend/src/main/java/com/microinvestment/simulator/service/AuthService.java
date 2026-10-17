package com.microinvestment.simulator.service;

import com.microinvestment.simulator.config.JwtService;
import com.microinvestment.simulator.dto.*;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.entity.Wallet;
import com.microinvestment.simulator.exception.*;
import com.microinvestment.simulator.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.users = users;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.name().trim().length() < 2) {
            throw new BusinessRuleException("Name must contain at least two non-padding characters.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResourceConflictException("An account with this email already exists.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessRuleException("Password must contain at most 72 UTF-8 bytes.");
        }
        User user = users.saveAndFlush(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        wallets.saveAndFlush(new Wallet(user));
        return auth(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        User user = users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        return auth(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    private AuthResponse auth(User user) {
        return new AuthResponse(jwt.generateToken(user), user.getId(), user.getName(), user.getEmail());
    }
}
