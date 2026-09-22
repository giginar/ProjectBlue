package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;

/** Plain-language disclosure for the current offline build. */
public final class PrivacyScreen extends StageMenuScreen {
    public PrivacyScreen(ProjectBlueGame game) {
        super(game, game.i18n().text("privacy.title"), game.i18n().text("privacy.subtitle"));
        note(t("privacy.local"));
        if (game.platform().ads().isSupported()) {
            note(t("privacy.ads_data")); note(t("privacy.ads_optional")); note(t("privacy.ads_options"));
        } else note(t("privacy.no_ads"));
        note(t("privacy.haptic")); note(t("privacy.reset")); note(t("privacy.local_ads")); note(t("privacy.contact"));
    }
}
