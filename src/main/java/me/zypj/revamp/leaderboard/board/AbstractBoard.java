package me.zypj.revamp.leaderboard.board;

import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public abstract class AbstractBoard<T> {

    private final String boardKey;
    private final BoardSource<T> source;

    public AbstractBoard(String boardKey, BoardSource<T> source) {
        this.boardKey = boardKey;
        this.source = source;
    }

    protected BoardSource<T> getSource() {
        return source;
    }

    public List<GenericBoardEntry<T>> getTop(int limit) {
        Map<T, Double> values = source.fetchCurrentValues();
        return values.entrySet().stream()
                .map(e -> new GenericBoardEntry<>(e.getKey(), e.getValue()))
                .sorted(Comparator.<GenericBoardEntry<T>, Double>comparing(GenericBoardEntry::getValue)
                        .reversed())
                .limit(limit > 0 ? limit : values.size())
                .collect(Collectors.toList());
    }

    public List<GenericBoardEntry<T>> getAll() {
        return getTop(0);
    }
}
