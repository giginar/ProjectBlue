package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;

public final class CreditsScreen extends StageMenuScreen {
    public CreditsScreen(ProjectBlueGame game) {
        super(game, "Credits", "Project Blue / ocean recovery prototype");
        note("Design and development\nProject Blue team");
        note("Visuals and audio\nOriginal procedural placeholder artwork, Blue Grid font and synthesized sounds created for this project.");
        note("Technology\nJava, libGDX and LWJGL.\nlibGDX: Apache License 2.0\nLWJGL: BSD 3-Clause License");
        note("All ten sectors use the shared data-driven recovery simulation. NEREID Core concludes the campaign while every restored sector remains replayable.");
        note("Build " + BuildInfo.VERSION_NAME);
    }
}
