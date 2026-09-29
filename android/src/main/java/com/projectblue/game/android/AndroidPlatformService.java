package com.projectblue.game.android;
import com.badlogic.gdx.Gdx;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.platform.PlatformService;
import com.projectblue.game.BuildConfig;
import android.app.Activity;
import com.projectblue.game.platform.AdsService;
import com.projectblue.game.platform.ConsentService;
public final class AndroidPlatformService extends NoOpPlatformService {
    private final AdsService ads;
    private final ConsentService consent;
    public AndroidPlatformService(Activity activity) {
        super(activity.getFilesDir().getAbsolutePath());
        if (BuildConfig.ADS_ENABLED) {
            AndroidConsentService enabledConsent = new AndroidConsentService(activity);
            AndroidAdsService enabledAds = new AndroidAdsService(activity, enabledConsent);
            enabledConsent.onChanged(enabledAds::consentChanged);
            consent = enabledConsent;
            ads = enabledAds;
        } else {
            consent = super.consent();
            ads = super.ads();
        }
    }
    @Override public AdsService ads() { return ads; }
    @Override public ConsentService consent() { return consent; }
    public void resume() { if (ads instanceof AndroidAdsService enabled) enabled.resume(); }
    public void pause() { if (ads instanceof AndroidAdsService enabled) enabled.pause(); }
    public void destroy() {
        if (consent instanceof AndroidConsentService enabled) enabled.destroy();
        if (ads instanceof AndroidAdsService enabled) enabled.destroy();
    }
    @Override public boolean developmentBuild() { return BuildConfig.DEBUG; }
    @Override public void haptic(PlatformService.Haptic kind) {
        Gdx.input.vibrate(switch (kind) { case LIGHT -> 18; case DAMAGE -> 45; case SUCCESS -> 28; });
    }
}
