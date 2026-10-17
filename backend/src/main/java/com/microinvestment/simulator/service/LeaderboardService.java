package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.LeaderboardEntryDto;
import com.microinvestment.simulator.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardService {
    private final UserRepository users;

    public LeaderboardService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard() {
        return users.findLeaderboard().stream().map(row -> new LeaderboardEntryDto(row.getUserId(),
                row.getName(), row.getTotalValue(), row.getInvested(), row.getPnl(), row.getReturnRate(),
                row.getTradesCount(), row.getRank())).toList();
    }
}
