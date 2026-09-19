package com.projectblue.game.screens;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.save.LevelRecord;
import com.projectblue.game.ui.Palette;

public final class LevelSelectScreen extends StageMenuScreen {
    private boolean briefing;
    public LevelSelectScreen(ProjectBlueGame game) {
        super(game, "Level Select", "Recovery chart / ten ocean sectors");
        showChart();
    }
    private void showChart() {
        briefing = false; body.clearChildren();
        note("Earn one star to open the next sector.\nRevisit any open sector to improve records.");
        for (CampaignConfig.Level level : CampaignConfig.DEFAULT.levels()) {
            LevelRecord record = profile.level(level.id());
            boolean available = CampaignConfig.isAvailable(level.id());
            Table card = panel();
            String state = available ? record.unlocked ? "" : " / LOCKED" : " / COMING LATER";
            TextButton choose = button("level-" + level.id(), String.format(java.util.Locale.ROOT, "%02d / %s%s", level.id(), level.name(), state), () -> {
                if (game.router().selectLevel(level.id())) showBriefing();
            });
            choose.setDisabled(!record.unlocked || !available);
            card.add(choose).height(84).row();
            card.add(label(level.region(), 1, Palette.AQUA)).row();
            card.add(rating(record.bestStars)).height(36).row();
            card.add(label("Best score " + record.bestScore, .92f, Palette.GOLD)).row();
            card.add(label("Cleared: " + completed(record), .82f, Palette.TEXT)).row();
            card.add(label("Cleanup " + Math.round(record.bestCleanup) + "% / Rescue " + Math.round(record.bestRescue) + "%", .82f, Palette.MUTED)).row();
            if (!available) card.add(label("Planned for a later content pass", .82f, Palette.MUTED)).row();
            else if (!record.unlocked) card.add(label("Complete sector " + (level.id() - 1) + " with 1+ star", .82f, Palette.MUTED)).row();
        }
        scrollToTop();
    }
    private void showBriefing() {
        briefing = true; body.clearChildren();
        int id = game.router().selectedLevel();
        CampaignConfig.Level level = CampaignConfig.DEFAULT.level(id);
        LevelRecord record = profile.level(id);
        Table summary = panel();
        summary.add(label(level.name(), 1.2f, Palette.AQUA)).row();
        MissionConfig mission = RunSpec.create(id, Difficulty.NORMAL, Loadout.standard()).mission();
        summary.add(label(level.region() + (mission == null ? " / 180 seconds" : " / 5-8 minutes"), .95f, Palette.TEXT)).row();
        summary.add(rating(record.bestStars)).height(36).row();
        summary.add(label("Best score " + record.bestScore, .95f, Palette.GOLD)).row();
        summary.add(label("Best difficulty: " + (record.bestDifficulty() == null ? "None" : record.bestDifficulty()), .88f, Palette.TEXT)).row();
        note("Complete each difficulty to open the next.\nNormal > Hard > Expert > Abyss");
        Table choices = new Table();
        choices.defaults().growX().height(84).space(10);
        for (Difficulty difficulty : Difficulty.values()) {
            boolean open = record.canPlay(difficulty);
            String state = !open ? "Locked" : record.completed(difficulty) ? "Cleared" : "Ready";
            TextButton select = button("difficulty-" + difficulty, difficulty + " / " + state, () -> {
                game.router().selectDifficulty(difficulty); showBriefing();
            });
            select.setDisabled(!open); select.setChecked(game.router().selectedDifficulty() == difficulty);
            choices.add(select).uniformX();
            if (difficulty.ordinal() % 2 == 1) choices.row();
        }
        body.add(choices).growX().row();
        Difficulty selected = game.router().selectedDifficulty();
        CampaignConfig.Tuning tuning = CampaignConfig.DEFAULT.tuning(selected);
        note(String.format(java.util.Locale.ROOT, "%s: hull x%.2f / shots x%.2f\nDensity x%.2f / fire rate x%.2f", selected, tuning.health(), tuning.bulletSpeed(), tuning.spawnDensity(), tuning.fireRate()));
        note(mission != null ? mission.briefing : level.boss() || selected != Difficulty.NORMAL
            ? "Disable the Warden before the timer ends. Watch for its golden firing signal."
            : "Survive, clean plastic and free turtles. Drag to steer; firing is automatic.");
        action("launch", "Begin dive / " + selected, () -> game.router().request(ScreenRouter.Route.PLAY));
        scrollToTop();
    }
    private static String completed(LevelRecord record) {
        StringBuilder text = new StringBuilder();
        for (Difficulty d : Difficulty.values()) if (record.completed(d)) text.append(d).append(' ');
        return text.length() == 0 ? "None" : text.toString().trim();
    }
    @Override protected void back() { if (briefing) showChart(); else super.back(); }
}
