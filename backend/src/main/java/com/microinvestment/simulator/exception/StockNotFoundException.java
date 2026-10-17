package com.microinvestment.simulator.exception;

public class StockNotFoundException extends ResourceNotFoundException {
    public StockNotFoundException(String message) {
        super(message);
    }
}
