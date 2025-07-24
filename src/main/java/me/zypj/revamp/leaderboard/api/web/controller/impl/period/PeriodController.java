package me.zypj.revamp.leaderboard.api.web.controller.impl.period;

import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${app.base-path:/api}/periods")
public class PeriodController {

    @GetMapping
    public List<String> listPeriods() {
        return Arrays.stream(PeriodType.values())
                .map(pt -> pt.name().toLowerCase())
                .collect(Collectors.toList());
    }
}
