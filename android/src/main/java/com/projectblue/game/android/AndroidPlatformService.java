package com.projectblue.game.android;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.BuildConfig;
public final class AndroidPlatformService extends NoOpPlatformService {
    public AndroidPlatformService(String filesDirectory) { super(filesDirectory); }
    @Override public boolean developmentBuild() { return BuildConfig.DEBUG; }
}
