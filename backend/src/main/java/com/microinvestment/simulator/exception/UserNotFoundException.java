package com.microinvestment.simulator.exception;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
