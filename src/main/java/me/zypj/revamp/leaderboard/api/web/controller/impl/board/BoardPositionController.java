package me.zypj.revamp.leaderboard.api.web.controller.impl.board;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${app.base-path:/api}/boards")
public class BoardPositionController {
    private final LeaderboardPlugin plugin;

    public BoardPositionController(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @GetMapping("/{key}/{period}/position/{uuid}")
    public int getPosition(
            @PathVariable String key,
            @PathVariable String period,
            @PathVariable UUID uuid
    ) {
        PeriodType pt = PeriodType.valueOf(period.toUpperCase());
        List<BoardEntry> full = plugin.getBootstrap()
                .getBoardService()
                .getLeaderboard(key, pt, 0);
        for (int i = 0; i < full.size(); i++) {
            if (full.get(i).getUuid().equals(uuid.toString())) {
                return i + 1;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not on board");
    }
}
