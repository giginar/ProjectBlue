package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;

public final class CreditsScreen extends StageMenuScreen {
    public CreditsScreen(ProjectBlueGame game) {
        super(game, "Credits", "Project Blue / ocean recovery prototype");
        note("Design and development\nProject Blue team");
        note("Visuals and audio\nOriginal procedural placeholder artwork, Blue Grid font and synthesized sounds created for this project.");
        note("Technology\nJava, libGDX and LWJGL.\nlibGDX: Apache License 2.0\nLWJGL: BSD 3-Clause License");
        note("Blue Coast uses the shared data-driven recovery simulation. Later sectors remain in planning. Art and balancing remain in development.");
        note("Build " + BuildInfo.VERSION_NAME);
    }
}
