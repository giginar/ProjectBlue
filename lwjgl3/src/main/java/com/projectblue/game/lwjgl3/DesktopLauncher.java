package com.projectblue.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.*;
import com.projectblue.game.ProjectBlueGame;

public final class DesktopLauncher {
    public static void main(String[] args) {
        boolean smoke = args.length > 0 && args[0].equals("--smoke");
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Project Blue");
        config.setWindowedMode(486, 864);
        config.setWindowSizeLimits(270, 480, -1, -1);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(15);
        config.setAudioConfig(16, 512, 9);
        config.setPauseWhenLostFocus(!smoke);
        config.setPauseWhenMinimized(true);
        ProjectBlueGame game = smoke ? new DesktopSmokeGame() : new ProjectBlueGame(new DesktopPlatformService());
        new Lwjgl3Application(game, config);
    }
}
