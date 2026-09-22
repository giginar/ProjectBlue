package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;

public final class CreditsScreen extends StageMenuScreen {
    public CreditsScreen(ProjectBlueGame game) {
        super(game, game.i18n().text("credits.title"), "Project Blue: Ocean Guard");
        note(t("credits.publisher"));
        note(t("credits.support"));
        note(t("credits.assets"));
        note(t("credits.tech"));
        note(t("credits.campaign"));
        note(t("menu.build", BuildInfo.VERSION_NAME));
    }
}
