package com.microinvestment.simulator.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class CustomPrincipal implements Serializable {
    private final Long id;
    private final String username;
}
