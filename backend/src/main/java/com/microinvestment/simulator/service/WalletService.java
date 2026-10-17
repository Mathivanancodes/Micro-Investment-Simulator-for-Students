package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.WalletResponse;
import com.microinvestment.simulator.entity.*;
import com.microinvestment.simulator.exception.*;
import com.microinvestment.simulator.repository.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final WalletRepository wallets;
    private final TransactionRepository transactions;

    public WalletService(WalletRepository wallets, TransactionRepository transactions) {
        this.wallets = wallets;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public WalletResponse get(Long userId) {
        return response(requireWallet(userId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletResponse deposit(Long userId, BigDecimal amount) {
        Wallet wallet = requireWallet(userId);
        wallet.credit(amount);
        wallets.saveAndFlush(wallet);
        transactions.saveAndFlush(new Transaction(wallet.getUser(), null, TransactionType.DEPOSIT,
                BigDecimal.ZERO, BigDecimal.ZERO, amount.setScale(2)));
        return response(wallet);
    }

    private Wallet requireWallet(Long id) {
        return wallets.findByUserId(id).orElseThrow(() -> new ResourceNotFoundException("Wallet not found."));
    }

    private WalletResponse response(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getUser().getId(), wallet.getBalance(), wallet.getVersion());
    }
}
