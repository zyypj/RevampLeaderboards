package me.zypj.revamp.leaderboard.api.web.controller.impl.player;

import lombok.Getter;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.api.web.controller.AbstractApiController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${app.base-path:/api}/players")
public class PlayerController extends AbstractApiController {

    public PlayerController(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @GetMapping
    public List<PlayerDto> getOnline() {
        return plugin
                .getServer()
                .getOnlinePlayers()
                .stream()
                .map(p -> new PlayerDto(p.getUniqueId(), p.getName()))
                .collect(Collectors.toList());
    }

    @Getter
    public static class PlayerDto {
        private final UUID uuid;
        private final String name;

        public PlayerDto(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
