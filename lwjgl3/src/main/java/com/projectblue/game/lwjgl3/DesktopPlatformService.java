package com.projectblue.game.lwjgl3;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.config.BuildInfo;
public final class DesktopPlatformService extends NoOpPlatformService {
    public DesktopPlatformService() {
        super(System.getProperty("user.home") + "/.projectblue");
    }
    @Override public boolean developmentBuild() { return BuildInfo.DEVELOPMENT_BUILD; }
}
