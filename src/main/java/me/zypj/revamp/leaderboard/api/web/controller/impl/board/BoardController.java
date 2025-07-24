package me.zypj.revamp.leaderboard.api.web.controller.impl.board;

import lombok.Getter;
import lombok.var;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.api.web.controller.AbstractApiController;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${app.base-path:/api}/boards")
public class BoardController extends AbstractApiController {

    public BoardController(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @GetMapping("/{key}")
    public PaginatedBoardResponse getBoard(
            @PathVariable String key,
            @RequestParam(defaultValue = "total") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PeriodType pt = PeriodType.valueOf(period.toUpperCase());
        List<BoardEntry> allEntries = plugin.getBootstrap()
                .getBoardService()
                .getLeaderboard(key, pt, 0);

        int totalItems = allEntries.size();
        if (size <= 0) {
            var items = allEntries.stream()
                    .map(e -> new BoardEntryDto(e.getUuid(), e.getPlayerName(), e.getValue()))
                    .collect(Collectors.toList());
            return new PaginatedBoardResponse(totalItems, 1, 0, totalItems, items);
        }

        int totalPages = (int) Math.ceil((double) totalItems / size);
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = currentPage * size;
        int to = Math.min(from + size, totalItems);

        List<BoardEntryDto> pageItems = allEntries.subList(from, to).stream()
                .map(e -> new BoardEntryDto(e.getUuid(), e.getPlayerName(), e.getValue()))
                .collect(Collectors.toList());

        return new PaginatedBoardResponse(totalItems, totalPages, currentPage, size, pageItems);
    }

    @GetMapping
    public List<String> listBoards() {
        return plugin.getBootstrap()
                .getBoardsConfigAdapter()
                .getBoards();
    }

    @Getter
    public static class PaginatedBoardResponse {
        private final int totalItems;
        private final int totalPages;
        private final int currentPage;
        private final int pageSize;
        private final List<BoardEntryDto> items;

        public PaginatedBoardResponse(int totalItems, int totalPages, int currentPage, int pageSize, List<BoardEntryDto> items) {
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.items = items;
        }
    }

    @Getter
    public static class BoardEntryDto {
        private final String uuid;
        private final String playerName;
        private final double value;

        public BoardEntryDto(String uuid, String playerName, double value) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.value = value;
        }
    }
}
