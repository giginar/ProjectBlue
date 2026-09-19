package com.projectblue.game.android;

import android.app.Activity;
import android.os.SystemClock;
import com.badlogic.gdx.Gdx;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.*;
import com.google.android.gms.ads.rewarded.*;
import com.projectblue.game.BuildConfig;
import com.projectblue.game.platform.AdsService;
import com.projectblue.game.platform.ConsentService;
import com.projectblue.game.platform.RewardedCompletion;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** All ad objects and SDK load/show calls are confined to the Android UI thread. */
public final class AndroidAdsService implements AdsService {
    private static final long MAX_AGE_MS = 55 * 60_000L, RETRY_MS = 30_000L;
    private final Activity activity;
    private final ConsentService consent;
    private RewardedAd rewarded;
    private InterstitialAd interstitial;
    private volatile boolean rewardedReady, interstitialReady, showing, resumed, destroyed;
    private volatile long rewardedAt, interstitialAt;
    private boolean initializing, initialized, loadingReward, loadingInterstitial;
    private long nextRewardAttempt, nextInterstitialAttempt;
    private int consentGeneration;
    private boolean adsAllowed;
    private Runnable cancelShowing;
    public AndroidAdsService(Activity activity, ConsentService consent) { this.activity = activity; this.consent = consent; }
    public boolean isSupported() { return BuildConfig.ADS_ENABLED; }
    public boolean isRewardedAvailable() { return usable() && rewardedReady && fresh(rewardedAt); }
    public boolean isInterstitialAvailable() { return usable() && interstitialReady && fresh(interstitialAt); }
    private boolean usable() { return !destroyed && resumed && !showing && consent.canRequestAds(); }
    private static boolean fresh(long loaded) { return SystemClock.elapsedRealtime() - loaded < MAX_AGE_MS; }
    public void resume() { resumed = true; preload(); }
    public void pause() { resumed = false; }
    public void consentChanged() {
        boolean allowed = consent.canRequestAds();
        if (allowed == adsAllowed) { preload(); return; }
        adsAllowed = allowed;
        // Drop ads loaded with earlier choices, including in-flight callbacks.
        consentGeneration++;
        rewarded = null; interstitial = null; rewardedReady = interstitialReady = false;
        loadingReward = loadingInterstitial = false;
        nextRewardAttempt = nextInterstitialAttempt = 0;
        preload();
    }
    public void preload() { activity.runOnUiThread(this::load); }
    private void load() {
        if (!usable() || !BuildConfig.ADS_ENABLED) return;
        if (!initialized) {
            if (initializing) return;
            initializing = true;
            MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setAgeRestrictedTreatment(AgeRestrictedTreatment.valueOf(BuildConfig.AD_AGE_TREATMENT))
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G).build());
            Thread initializer = new Thread(() -> {
                try {
                    MobileAds.initialize(activity.getApplicationContext(), status -> activity.runOnUiThread(() -> {
                        if (destroyed) return;
                        initializing = false; initialized = true; load();
                    }));
                } catch (RuntimeException error) { activity.runOnUiThread(() -> initializing = false); }
            }, "ads-initialization");
            initializer.start();
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (rewarded != null && !fresh(rewardedAt)) { rewarded = null; rewardedReady = false; }
        if (interstitial != null && !fresh(interstitialAt)) { interstitial = null; interstitialReady = false; }
        int generation = consentGeneration;
        if (rewarded == null && !loadingReward && now >= nextRewardAttempt) {
            loadingReward = true; nextRewardAttempt = now + RETRY_MS;
            try { RewardedAd.load(activity, BuildConfig.REWARDED_AD_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                @Override public void onAdLoaded(RewardedAd ad) {
                    if (generation != consentGeneration || destroyed) return;
                    loadingReward = false;
                    if (consent.canRequestAds()) { rewarded = ad; rewardedAt = SystemClock.elapsedRealtime(); rewardedReady = true; }
                }
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    if (generation == consentGeneration) { loadingReward = false; rewardedReady = false; }
                }
            }); } catch (RuntimeException error) { loadingReward = false; }
        }
        if (interstitial == null && !loadingInterstitial && now >= nextInterstitialAttempt) {
            loadingInterstitial = true; nextInterstitialAttempt = now + RETRY_MS;
            try { InterstitialAd.load(activity, BuildConfig.INTERSTITIAL_AD_ID, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(InterstitialAd ad) {
                    if (generation != consentGeneration || destroyed) return;
                    loadingInterstitial = false;
                    if (consent.canRequestAds()) { interstitial = ad; interstitialAt = SystemClock.elapsedRealtime(); interstitialReady = true; }
                }
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    if (generation == consentGeneration) { loadingInterstitial = false; interstitialReady = false; }
                }
            }); } catch (RuntimeException error) { loadingInterstitial = false; }
        }
    }
    public void showRewarded(Consumer<Boolean> completion) {
        activity.runOnUiThread(() -> {
            if (!isRewardedAvailable() || rewarded == null || activity.isFinishing()) { post(() -> completion.accept(false)); load(); return; }
            RewardedAd ad = rewarded; rewarded = null; rewardedReady = false; showing = true;
            RewardedCompletion events = new RewardedCompletion(earned -> {
                showing = false; cancelShowing = null;
                post(() -> completion.accept(earned)); load();
            });
            cancelShowing = events::fail;
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() { events.dismiss(); }
                @Override public void onAdFailedToShowFullScreenContent(AdError error) { events.fail(); }
            });
            try { ad.show(activity, reward -> events.earned()); } catch (RuntimeException error) { events.fail(); }
        });
    }
    public void showInterstitial(Runnable completion) {
        activity.runOnUiThread(() -> {
            if (!isInterstitialAvailable() || interstitial == null || activity.isFinishing()) { post(completion); load(); return; }
            InterstitialAd ad = interstitial; interstitial = null; interstitialReady = false; showing = true;
            AtomicBoolean once = new AtomicBoolean();
            Runnable finish = () -> {
                if (!once.compareAndSet(false, true)) return;
                showing = false; cancelShowing = null; post(completion); load();
            };
            cancelShowing = finish;
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() { finish.run(); }
                @Override public void onAdFailedToShowFullScreenContent(AdError error) { finish.run(); }
            });
            try { ad.show(activity); } catch (RuntimeException error) { finish.run(); }
        });
    }
    public void destroy() {
        destroyed = true; consentGeneration++;
        rewarded = null; interstitial = null; rewardedReady = interstitialReady = false;
        if (cancelShowing != null) cancelShowing.run();
    }
    private static void post(Runnable runnable) { if (Gdx.app != null) Gdx.app.postRunnable(runnable); }
}
