package com.projectblue.game.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.save.LevelRecord;
import com.projectblue.game.ui.Palette;

/** Sector cards are the launch action; difficulty remains selectable per open sector. */
public final class LevelSelectScreen extends StageMenuScreen {
    private final Difficulty[] choices = new Difficulty[CampaignConfig.LEVEL_COUNT];
    private int focusedLevel = 1;
    public LevelSelectScreen(ProjectBlueGame game) {
        super(game, game.i18n().text("levels.title"), game.i18n().text("levels.subtitle"));
        java.util.Arrays.fill(choices, Difficulty.NORMAL);
        showChart();
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.BUTTON_A) return start(focusedLevel);
                return false;
            }
        });
    }
    private void showChart() {
        body.clearChildren(); note(t("levels.help"));
        for (CampaignConfig.Level level : CampaignConfig.DEFAULT.levels()) {
            int id = level.id(); LevelRecord record = profile.level(id);
            boolean available = CampaignConfig.isAvailable(id), unlocked = LevelSelectPolicy.canStart(profile, id, Difficulty.NORMAL);
            Table card = panel();
            String state = !available ? t("levels.coming") : !unlocked ? t("levels.locked") : "";
            TextButton dive = button("level-" + id, t("levels.card", String.format(java.util.Locale.ROOT, "%02d", id), levelName(id), state), () -> start(id));
            dive.setDisabled(!unlocked);
            dive.addListener(new InputListener() {
                @Override public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor from) { focusedLevel = id; }
            });
            card.add(dive).height(84).row();
            String status = !available ? t("levels.planned") : !unlocked ? t("common.locked") : record.bestStars > 0 ? t("levels.restored") : t("levels.open");
            String difficultyStatus = record.bestDifficulty() == null ? t("levels.normal_ready") : t("levels.difficulty_cleared", difficulty(record.bestDifficulty()));
            card.add(label(t("levels.status", status, difficultyStatus), .78f, unlocked ? Palette.AQUA : Palette.MUTED)).row();
            card.add(label(region(id), 1, Palette.AQUA)).row();
            card.add(label(t("level." + id + ".description"), .82f, Palette.TEXT)).row();
            card.add(rating(record.bestStars)).height(36).row();
            card.add(label(t("levels.best_score", record.bestScore), .92f, Palette.GOLD)).row();
            card.add(label(t("levels.cleared", completed(record)), .82f, Palette.TEXT)).row();
            card.add(label(t("levels.performance", Math.round(record.bestCleanup), Math.round(record.bestRescue)), .82f, Palette.MUTED)).row();
            if (!available) card.add(label(t("levels.later"), .82f, Palette.MUTED)).row();
            else if (!unlocked) card.add(label(t("levels.requirement", id - 1), .82f, Palette.MUTED)).row();
            else {
                card.add(label(t("levels.tap"), .78f, Palette.GOLD)).row();
                TextButton difficultyButton = button("difficulty-" + id, difficulty(choices[id - 1]), () -> { choices[id - 1] = nextDifficulty(record, choices[id - 1]); showChart(); });
                card.add(difficultyButton).height(62).row();
            }
        }
        scrollToTop();
        com.badlogic.gdx.scenes.scene2d.Actor focus = stage.getRoot().findActor("level-" + focusedLevel);
        if (focus != null) stage.setKeyboardFocus(focus);
    }
    private boolean start(int id) {
        focusedLevel = id;
        return LevelSelectPolicy.canStart(profile, id, choices[id - 1]) && game.router().requestDive(id, choices[id - 1]);
    }
    private Difficulty nextDifficulty(LevelRecord record, Difficulty current) {
        Difficulty[] all = Difficulty.values();
        for (int offset = 1; offset <= all.length; offset++) {
            Difficulty candidate = all[(current.ordinal() + offset) % all.length];
            if (record.canPlay(candidate)) return candidate;
        }
        return Difficulty.NORMAL;
    }
    private String completed(LevelRecord record) {
        StringBuilder text = new StringBuilder();
        for (Difficulty d : Difficulty.values()) if (record.completed(d)) { if (text.length() > 0) text.append(" / "); text.append(difficulty(d)); }
        return text.length() == 0 ? t("common.none") : text.toString();
    }
    private String difficulty(Difficulty value) { return t("difficulty." + value.name()); }
    private String levelName(int id) { return t("level." + id + ".name"); }
    private String region(int id) { return t("level." + id + ".region"); }
}
