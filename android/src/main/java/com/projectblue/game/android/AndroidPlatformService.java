package com.projectblue.game.android;
import com.badlogic.gdx.Gdx;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.platform.PlatformService;
import com.projectblue.game.BuildConfig;
import android.app.Activity;
import com.projectblue.game.platform.AdsService;
import com.projectblue.game.platform.ConsentService;
public final class AndroidPlatformService extends NoOpPlatformService {
    private final AndroidAdsService ads;
    private final AndroidConsentService consent;
    public AndroidPlatformService(Activity activity) {
        super(activity.getFilesDir().getAbsolutePath());
        consent = new AndroidConsentService(activity);
        ads = new AndroidAdsService(activity, consent);
        consent.onChanged(ads::consentChanged);
    }
    @Override public AdsService ads() { return ads; }
    @Override public ConsentService consent() { return consent; }
    public void resume() { ads.resume(); }
    public void pause() { ads.pause(); }
    public void destroy() { consent.destroy(); ads.destroy(); }
    @Override public boolean developmentBuild() { return BuildConfig.DEBUG; }
    @Override public void haptic(PlatformService.Haptic kind) {
        Gdx.input.vibrate(switch (kind) { case LIGHT -> 18; case DAMAGE -> 45; case SUCCESS -> 28; });
    }
}
