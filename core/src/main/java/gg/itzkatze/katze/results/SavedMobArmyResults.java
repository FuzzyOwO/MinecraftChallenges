package gg.itzkatze.katze.results;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SavedMobArmyResults {
    private SavedMobArmyResults() {}

    public static List<SavedBattle.Objective> group(List<SavedBattle.Objective> objectives) {
        Map<String, SavedBattle.Objective> groups = new LinkedHashMap<>();
        for (SavedBattle.Objective objective : objectives) {
            groups.merge(objective.icon(), objective, (first, next) -> new SavedBattle.Objective(first.name(), first.type(), first.icon(),
                    first.finishedBy(), first.remainingSeconds(), first.finished(), first.skipped(), first.count() + next.count()));
        }
        return groups.values().stream().sorted(Comparator.comparing(SavedBattle.Objective::icon)).toList();
    }
}
