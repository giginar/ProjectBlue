package com.projectblue.game.android;

import android.app.Activity;
import com.badlogic.gdx.Gdx;
import com.google.android.ump.*;
import com.projectblue.game.BuildConfig;
import com.projectblue.game.platform.ConsentGate;
import com.projectblue.game.platform.ConsentService;
import java.util.concurrent.atomic.AtomicBoolean;

/** UMP owns region and consent decisions. No locally inferred consent or geography. */
public final class AndroidConsentService implements ConsentService {
    private final Activity activity;
    private final ConsentInformation information;
    private final ConsentGate gate = new ConsentGate();
    private Runnable changed = () -> {};
    private boolean started, busy, destroyed;
    private volatile boolean privacyRequired;
    public AndroidConsentService(Activity activity) {
        this.activity = activity;
        information = UserMessagingPlatform.getConsentInformation(activity);
    }
    public void onChanged(Runnable changed) { this.changed = changed; }
    public boolean canRequestAds() { return BuildConfig.ADS_ENABLED && gate.canRequestAds(); }
    public boolean isPrivacyOptionsRequired() { return privacyRequired; }
    public void requestConsent(Runnable completion) {
        activity.runOnUiThread(() -> {
            if (destroyed || started || !BuildConfig.ADS_ENABLED) { post(completion); return; }
            started = true; busy = true; gate.begin();
            ConsentRequestParameters.Builder parameters = new ConsentRequestParameters.Builder();
            if (BuildConfig.UMP_UNDER_AGE >= 0) parameters.setTagForUnderAgeOfConsent(BuildConfig.UMP_UNDER_AGE == 1);
            AtomicBoolean done = new AtomicBoolean();
            Runnable finish = () -> {
                if (!done.compareAndSet(false, true) || destroyed) return;
                busy = false; gate.formOpen(false); refresh(); post(completion);
            };
            try {
                information.requestConsentInfoUpdate(activity, parameters.build(), () -> {
                    if (destroyed) return;
                    gate.formOpen(true); changed.run();
                    try { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> finish.run()); }
                    catch (RuntimeException error) { finish.run(); }
                }, error -> finish.run());
                // UMP may authorize requests using the previous session after update was requested.
                refresh();
            } catch (RuntimeException error) { finish.run(); }
        });
    }
    public void showPrivacyOptions(Runnable completion) {
        activity.runOnUiThread(() -> {
            if (destroyed || busy || !privacyRequired) { post(completion); return; }
            busy = true; gate.formOpen(true); changed.run();
            AtomicBoolean done = new AtomicBoolean();
            Runnable finish = () -> {
                if (!done.compareAndSet(false, true) || destroyed) return;
                busy = false; gate.formOpen(false); refresh(); post(completion);
            };
            try { UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> finish.run()); }
            catch (RuntimeException error) { finish.run(); }
        });
    }
    private void refresh() {
        if (destroyed) return;
        gate.update(information.canRequestAds());
        privacyRequired = information.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        changed.run();
    }
    public void destroy() { destroyed = true; gate.begin(); changed = () -> {}; }
    private static void post(Runnable runnable) { if (Gdx.app != null) Gdx.app.postRunnable(runnable); }
}
