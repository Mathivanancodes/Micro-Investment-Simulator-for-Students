package com.microinvestment.simulator.exception;

public class SimulatorException extends RuntimeException {
    public SimulatorException(String message) {
        // INTERVIEW: super(message) preserves the explanation in the RuntimeException hierarchy.
        super(message);
    }

    public SimulatorException(String message, Throwable cause) {
        // INTERVIEW: exception chaining preserves the original cause while adding domain context.
        super(message, cause);
    }
}
