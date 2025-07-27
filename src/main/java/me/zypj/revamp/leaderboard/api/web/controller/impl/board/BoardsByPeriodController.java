package me.zypj.revamp.leaderboard.api.web.controller.impl.board;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${app.base-path:/api}/boards/period")
public class BoardsByPeriodController {
    private final LeaderboardPlugin plugin;

    public BoardsByPeriodController(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @GetMapping("/{period}")
    public Map<String, List<BoardController.BoardEntryDto>> getAllByPeriod(
            @PathVariable String period,
            @RequestParam(defaultValue = "0") int limit
    ) {
        PeriodType pt = PeriodType.valueOf(period.toUpperCase());
        List<String> keys = new ArrayList<>(plugin.getBootstrap()
                .getBoardsConfigAdapter()
                .getBoardKeys());

        return keys.stream().collect(Collectors.toMap(
                key -> key,
                key -> plugin.getBootstrap()
                        .getBoardService()
                        .getLeaderboard(key, pt, limit)
                        .stream()
                        .map(e -> new BoardController.BoardEntryDto(e.getKey(), e.getDisplay(), e.getValue()))
                        .collect(Collectors.toList())
        ));
    }
}
