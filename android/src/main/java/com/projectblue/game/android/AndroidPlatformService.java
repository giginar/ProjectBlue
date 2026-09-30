package com.projectblue.game.android;
import com.badlogic.gdx.Gdx;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.platform.PlatformService;
import com.projectblue.game.BuildConfig;
import android.app.Activity;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import com.projectblue.game.platform.AdsService;
import com.projectblue.game.platform.ConsentService;
public final class AndroidPlatformService extends NoOpPlatformService {
    private final AdsService ads;
    private final ConsentService consent;
    private final Activity activity;
    private boolean active=true;
    public AndroidPlatformService(Activity activity) {
        super(activity.getFilesDir().getAbsolutePath());
        this.activity=activity;
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
    public void resume() { active=true; if (ads instanceof AndroidAdsService enabled) enabled.resume(); }
    public void pause() { active=false; if (ads instanceof AndroidAdsService enabled) enabled.pause(); }
    public void destroy() {
        if (consent instanceof AndroidConsentService enabled) enabled.destroy();
        if (ads instanceof AndroidAdsService enabled) enabled.destroy();
    }
    @Override public boolean developmentBuild() { return BuildConfig.DEBUG; }
    @Override public void haptic(PlatformService.Haptic kind) {
        if (!active) return;
        AudioManager audio=(AudioManager)activity.getSystemService(Activity.AUDIO_SERVICE);
        if (audio!=null&&audio.getRingerMode()==AudioManager.RINGER_MODE_SILENT) return;
        Vibrator vibrator=(Vibrator)activity.getSystemService(Activity.VIBRATOR_SERVICE);
        if (vibrator==null||!vibrator.hasVibrator()) return;
        long[] pattern=kind==PlatformService.Haptic.BOSS?new long[]{0,24,42,32}:null;
        int duration=switch (kind) { case LIGHT -> 12; case HEAVY -> 24; case DAMAGE -> 38; case SUCCESS -> 22; case BOSS -> 0; };
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            VibrationEffect effect=pattern==null?VibrationEffect.createOneShot(duration,VibrationEffect.DEFAULT_AMPLITUDE)
                :VibrationEffect.createWaveform(pattern,-1);
            vibrator.vibrate(effect);
        } else if (pattern==null) vibrator.vibrate(duration);
        else vibrator.vibrate(pattern,-1);
    }
}
