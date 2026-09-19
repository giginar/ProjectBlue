package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Upgrade;
import com.projectblue.game.ui.Palette;

public final class UpgradesScreen extends StageMenuScreen {
    private String message = "";
    public UpgradesScreen(ProjectBlueGame game) { super(game,"Upgrades","Permanent improvements / paid with salvage"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        if (!message.isEmpty()) note(message);
        for (Upgrade upgrade : Upgrade.values()) {
            var def = profile.content().upgrade(upgrade.name());
            int level = profile.upgradeLevel(upgrade), cost = def.cost(level);
            Table card = panel();
            card.add(label(def.displayName() + " / " + level + " of " + def.maxLevel(),1.05f,Palette.AQUA)).row();
            card.add(label(def.description(),.95f,Palette.TEXT)).row();
            TextButton buy = button("upgrade-" + upgrade,level == def.maxLevel() ? "Fully upgraded"
                : profile.totalSalvage < cost ? "Need " + cost + " salvage" : "Upgrade / " + cost + " salvage",() -> {
                    message = switch (game.saves().purchase(upgrade)) {
                        case PURCHASED -> "Upgrade installed and saved.";
                        case MAX_LEVEL -> "Already fully upgraded.";
                        case INSUFFICIENT_SALVAGE -> "Not enough salvage.";
                        case SAVE_FAILED -> "Purchase could not be saved. No salvage spent. Please retry.";
                    };
                    rebuild();
                });
            buy.setDisabled(level == def.maxLevel() || profile.totalSalvage < cost);
            card.add(buy).height(84).row();
        }
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
