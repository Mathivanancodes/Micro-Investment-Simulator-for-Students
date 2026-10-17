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
public class LeaderboardController {
    private final LeaderboardService leaderboard;
    public LeaderboardController(LeaderboardService leaderboard) { this.leaderboard = leaderboard; }
    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDto> get() { return leaderboard.getLeaderboard(); }
}
