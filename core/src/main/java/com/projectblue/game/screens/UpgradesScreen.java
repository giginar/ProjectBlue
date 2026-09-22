package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Upgrade;
import com.projectblue.game.ui.Palette;

public final class UpgradesScreen extends StageMenuScreen {
    private String message = "";
    public UpgradesScreen(ProjectBlueGame game) { super(game,game.i18n().text("upgrade.title"),game.i18n().text("upgrade.subtitle")); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        if (!message.isEmpty()) note(message);
        for (Upgrade upgrade : Upgrade.values()) {
            var def = profile.content().upgrade(upgrade.name());
            int level = profile.upgradeLevel(upgrade), cost = def.cost(level);
            Table card = panel();
            card.add(label(t("upgrade.level", contentName(upgrade), level, def.maxLevel()),1.05f,Palette.AQUA)).row();
            card.add(label(contentDescription(upgrade),.95f,Palette.TEXT)).row();
            String state=level == def.maxLevel() ? t("upgrade.maximum") : profile.totalSalvage < cost ? t("upgrade.need") : t("upgrade.available");
            card.add(label(state,.82f,level == def.maxLevel()?Palette.AQUA:profile.totalSalvage < cost?Palette.MUTED:Palette.GOLD)).row();
            TextButton buy = button("upgrade-" + upgrade,level == def.maxLevel() ? t("upgrade.max_button")
                : profile.totalSalvage < cost ? t("upgrade.need_button", cost) : t("upgrade.buy", cost),() -> {
                    message = switch (game.saves().purchase(upgrade)) {
                        case PURCHASED -> t("upgrade.purchased");
                        case MAX_LEVEL -> t("upgrade.already_max");
                        case INSUFFICIENT_SALVAGE -> t("upgrade.insufficient");
                        case SAVE_FAILED -> t("upgrade.save_failed");
                    };
                    rebuild();
                });
            buy.setDisabled(level == def.maxLevel() || profile.totalSalvage < cost);
            card.add(buy).height(84).row();
        }
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
