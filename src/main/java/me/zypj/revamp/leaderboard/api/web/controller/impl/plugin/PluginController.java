package me.zypj.revamp.leaderboard.api.web.controller.impl.plugin;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.api.web.controller.AbstractApiController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${app.base-path:/api}/plugin")
public class PluginController extends AbstractApiController {

    public PluginController(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @GetMapping
    public Map<String, Object> getInfo() {
        LeaderboardPlugin plugin = this.plugin;
        Map<String, Object> info = new HashMap<>();
        info.put("name", plugin.getDescription().getName());
        info.put("version", plugin.getDescription().getVersion());
        info.put("authors", plugin.getDescription().getAuthors());
        return info;
    }
}
