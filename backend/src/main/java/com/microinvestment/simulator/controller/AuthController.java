package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.config.CustomPrincipal;
import com.microinvestment.simulator.config.JwtUtil;
import com.microinvestment.simulator.dto.AuthRequest;
import com.microinvestment.simulator.dto.AuthResponse;
import com.microinvestment.simulator.dto.RegisterRequest;
import com.microinvestment.simulator.model.User;
import com.microinvestment.simulator.model.Wallet;
import com.microinvestment.simulator.repository.UserRepository;
import com.microinvestment.simulator.repository.WalletRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserRepository userRepository,
                          WalletRepository walletRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /api/auth/register
     * Register a new user, hash password, and initialize their portfolio wallet with $10,000.00.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Username already exists.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Email already exists.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        // 1. Create User
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_STUDENT")
                .build();
        User savedUser = userRepository.save(user);

        // 2. Initialize Wallet with default $10,000.00
        BigDecimal defaultStartingBalance = new BigDecimal("10000.00");
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(defaultStartingBalance)
                .startingBalance(defaultStartingBalance)
                .periodStartBalance(defaultStartingBalance)
                .build();
        walletRepository.save(wallet);

        log.info("User registered successfully. User ID: {}", savedUser.getId());

        // Auto login after registration and return token details
        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId(), savedUser.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .token(token)
                        .username(savedUser.getUsername())
                        .role(savedUser.getRole())
                        .userId(savedUser.getId())
                        .build()
        );
    }

    /**
     * POST /api/auth/login
     * Verifies username/password, generates and returns JWT on success.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody AuthRequest request) {
        log.info("Authentication request received for username: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Invalid username or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .username(user.getUsername())
                        .role(user.getRole())
                        .userId(user.getId())
                        .build()
        );
    }

    /**
     * GET /api/auth/me
     * Returns details of currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Object principalObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principalObj instanceof CustomPrincipal) {
            CustomPrincipal principal = (CustomPrincipal) principalObj;
            Map<String, Object> details = new HashMap<>();
            details.put("userId", principal.getId());
            details.put("username", principal.getUsername());
            return ResponseEntity.ok(details);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous access");
    }
}
