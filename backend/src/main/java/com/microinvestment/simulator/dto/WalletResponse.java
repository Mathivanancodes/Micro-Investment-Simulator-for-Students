package com.microinvestment.simulator.dto;

import java.math.BigDecimal;

public record WalletResponse(Long id, Long userId, BigDecimal balance, Long version) {
}
