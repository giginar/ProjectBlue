package com.projectblue.game.lwjgl3;
import com.projectblue.game.platform.NoOpPlatformService;
public final class DesktopPlatformService extends NoOpPlatformService {
    public DesktopPlatformService() {
        super(System.getProperty("user.home") + "/.projectblue");
    }
}

