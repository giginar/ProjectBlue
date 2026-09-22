package com.projectblue.game.ui;

import com.projectblue.game.save.*;
import com.projectblue.game.i18n.Localization;
import java.util.Locale;

/** Shared overlay survives menu transitions and queues simultaneous unlocks. */
public final class AchievementToast {
    private final LocalAchievementService service;
    private final UiPainter ui;
    private final Localization text;
    private float remaining, retry;
    private String title;
    public AchievementToast(LocalAchievementService service, UiPainter ui, Localization text) { this.service = service; this.ui = ui; this.text = text; }
    public void draw(float delta) {
        remaining -= delta; retry -= delta;
        if (remaining <= 0 && retry <= 0) {
            retry = .5f;
            Achievement next = service.nextNotification();
            if (next != null) { title = text.text("achievement." + next + ".name").toUpperCase(Locale.ROOT); remaining = 3.5f; }
        }
        if (remaining <= 0) return;
        ui.beginShapes(); ui.rect(16,720,508,66,Palette.INK); ui.endShapes();
        ui.beginText(); ui.centered(text.text("achievements.unlocked"),768,.55f,Palette.GOLD);
        ui.centered(title,740,.66f,Palette.TEXT); ui.endText();
    }
}
