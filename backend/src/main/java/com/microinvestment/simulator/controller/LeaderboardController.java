package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.dto.LeaderboardEntryDto;
import com.microinvestment.simulator.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @Autowired
    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * GET /api/leaderboard
     * Returns the list of ranked students and returns.
     */
    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboardStandings() {
        List<LeaderboardEntryDto> leaderboard = leaderboardService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * POST /api/leaderboard/reset
     * Resets the leaderboard period. Admin operation.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetLeaderboardPeriod() {
        leaderboardService.resetLeaderboard();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Leaderboard starting values successfully snapshotted and reset.");
        return ResponseEntity.ok(response);
    }
}
