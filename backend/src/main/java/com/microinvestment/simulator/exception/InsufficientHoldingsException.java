package com.microinvestment.simulator.exception;

public class InsufficientHoldingsException extends BusinessRuleException {
    public InsufficientHoldingsException(String message) {
        super(message);
    }
}
