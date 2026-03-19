package gg.itzkatze.mcc.allitems;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.bossbar.BossbarManager;
import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.allitems.data.AllItemsData;
import gg.itzkatze.mcc.allitems.gui.AllItemsSettingsGUI;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.scoreboard.ScoreboardContent;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class AllItems implements Challenge {

    @Getter
    private final static AllItemsData data = new AllItemsData();
    private final AllItemsGameListener gameListener = new AllItemsGameListener(data);
    private final ChallengeSettingsManager settingsManager =
            new ChallengeSettingsManager(this, AllItemsSettings.AllItemsSetting.class);

    @Override
    public void onEnable() {
        data.loadFromFile();

        gameListener.initialize();
    }

    @Override
    public void onDisable() {
        BossbarManager.hideBarsForChallenge(this);
        data.saveToFile();
        gameListener.cleanup();
    }

    @Override
    @NotNull public String getName() {
        return "allitems";
    }

    @Override
    @NotNull public Component getDisplayName() {
        return Component.text("All Items");
    }

    @Override
    public @NotNull ChallengeType getType() {
        return ChallengeType.ALL_ITEMS;
    }

    @Override
    @NotNull public Material getMaterial() {
        return Material.CHEST;
    }

    @Override
    @NotNull public String getDescription() {
        return "Collect every item in Minecraft";
    }

    @Override
    @NotNull public List<MccCommand> getCommands() {
        return MinecraftChallenges.loopThroughPackage(
                "gg.itzkatze.mcc.allitems.commands",
                MccCommand.class
        ).toList();
    }

    @Override
    @NotNull public List<Listener> getListeners() {
        return List.of(gameListener);
    }

    @Override
    @NotNull public List<Runnable> getTasks() {
        return List.of(data::saveToFile);
    }

    @Override
    public ScoreboardContent getScoreboardContent() {
        return new AllItemsScoreboardContent(data);
    }

    @Override
    public @NonNull List<BossbarContent> getBossbarContents() {
        return List.of(
                new AllItemsBossbarContent(data),
                new AllItemsChainBossbarContent(data)
        );
    }

    @Override
    public MccGUI getSettingsGUI() {
        return new AllItemsSettingsGUI();
    }

    @Override
    @Nullable public ChallengeSettingsManager getSettingsManager() {
        return settingsManager;
    }
}
