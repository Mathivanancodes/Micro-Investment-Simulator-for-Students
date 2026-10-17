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
public class WalletController {
    private final WalletService wallets;
    private final AccessPolicy access;
    public WalletController(WalletService wallets, AccessPolicy access) { this.wallets = wallets; this.access = access; }
    @GetMapping("/wallet/{userId}")
    public WalletResponse get(@PathVariable @Positive Long userId) {
        access.requireOwner(userId); return wallets.get(userId);
    }
    @PostMapping("/wallet/{userId}/deposit")
    public WalletResponse deposit(@PathVariable @Positive Long userId, @Valid @RequestBody DepositRequest request) {
        access.requireOwner(userId); return wallets.deposit(userId, request.amount());
    }
}
