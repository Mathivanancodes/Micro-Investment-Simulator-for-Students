package com.microinvestment.simulator.exception;

// INTERVIEW: a checked exception requires callers to catch it or declare it with throws.
public class SimulationExportException extends Exception {
    public SimulationExportException(String message) {
        super(message);
    }

    public SimulationExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
