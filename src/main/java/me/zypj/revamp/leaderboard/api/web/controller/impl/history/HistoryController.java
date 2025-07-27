package me.zypj.revamp.leaderboard.api.web.controller.impl.history;

import lombok.Getter;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.HistoricalEntry;
import me.zypj.revamp.leaderboard.api.web.controller.AbstractApiController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${app.base-path:/api}/history")
public class HistoryController extends AbstractApiController {

    public HistoryController(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @GetMapping("/board/{key}/{period}")
    public List<SnapshotDto> getBoardHistory(
            @PathVariable String key,
            @PathVariable String period,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        PeriodType pt = PeriodType.valueOf(period.toUpperCase());
        LocalDateTime f = Optional.ofNullable(from).orElse(LocalDateTime.now().minusDays(30));
        LocalDateTime t = Optional.ofNullable(to).orElse(LocalDateTime.now());

        List<HistoricalEntry> all = plugin.getBootstrap()
                .getArchiveRepository()
                .getHistory(key, pt, f, t);

        Map<LocalDateTime, List<HistoricalEntry>> grouped = all.stream()
                .collect(Collectors.groupingBy(HistoricalEntry::getSnapshotTime, TreeMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(e -> new SnapshotDto(
                        e.getKey(),
                        e.getValue().stream()
                                .map(HistoricalEntryDto::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/player/{uuid}/{key}/{period}")
    public List<PlayerHistoryDto> getPlayerHistory(
            @PathVariable UUID uuid,
            @PathVariable String key,
            @PathVariable String period,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        PeriodType pt = PeriodType.valueOf(period.toUpperCase());
        LocalDateTime f = Optional.ofNullable(from).orElse(LocalDateTime.now().minusDays(30));
        LocalDateTime t = Optional.ofNullable(to).orElse(LocalDateTime.now());

        return plugin.getBootstrap()
                .getArchiveRepository()
                .getPlayerHistory(uuid, key, pt, f, t)
                .stream()
                .map(PlayerHistoryDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class SnapshotDto {
        private final LocalDateTime snapshotTime;
        private final List<HistoricalEntryDto> entries;

        public SnapshotDto(LocalDateTime snapshotTime, List<HistoricalEntryDto> entries) {
            this.snapshotTime = snapshotTime;
            this.entries = entries;
        }
    }

    @Getter
    public static class HistoricalEntryDto {
        private final UUID uuid;
        private final String playerName;
        private final double value;

        public HistoricalEntryDto(HistoricalEntry e) {
            this.uuid = e.getPlayerUuid();
            this.playerName = e.getPlayerName();
            this.value = e.getValue();
        }
    }

    @Getter
    public static class PlayerHistoryDto {
        private final LocalDateTime snapshotTime;
        private final double value;

        public PlayerHistoryDto(HistoricalEntry e) {
            this.snapshotTime = e.getSnapshotTime();
            this.value = e.getValue();
        }
    }
}
