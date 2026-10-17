package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.config.AccessPolicy;
import com.microinvestment.simulator.dto.*;
import com.microinvestment.simulator.exception.SimulationExportException;
import com.microinvestment.simulator.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class AuthController {
    private final AuthService auth;
    private final AccessPolicy access;
    public AuthController(AuthService auth, AccessPolicy access) { this.auth = auth; this.access = access; }
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) { return auth.login(request); }
    @GetMapping("/auth/me")
    public UserResponse me() { return auth.me(access.currentUserId()); }
}
