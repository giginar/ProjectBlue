package com.projectblue.game.android;

import android.os.Build;
import android.os.Bundle;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.projectblue.game.ProjectBlueGame;

public final class AndroidLauncher extends AndroidApplication {
    private AndroidPlatformService platform;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useImmersiveMode = true;
        config.numSamples = 2;
        config.a = 8;
        platform = new AndroidPlatformService(this);
        View gameView = initializeForView(new ProjectBlueGame(platform), config);
        if (!com.projectblue.game.BuildConfig.DEBUG) setLogLevel(com.badlogic.gdx.Application.LOG_NONE);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff061827);
        root.addView(gameView);
        // API 36 edge-to-edge/cutouts: viewport receives only the usable content rectangle.
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets safe = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else {
                int left = windowInsets.getSystemWindowInsetLeft(), top = windowInsets.getSystemWindowInsetTop();
                int right = windowInsets.getSystemWindowInsetRight(), bottom = windowInsets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= 28 && windowInsets.getDisplayCutout() != null) {
                    left = Math.max(left, windowInsets.getDisplayCutout().getSafeInsetLeft());
                    top = Math.max(top, windowInsets.getDisplayCutout().getSafeInsetTop());
                    right = Math.max(right, windowInsets.getDisplayCutout().getSafeInsetRight());
                    bottom = Math.max(bottom, windowInsets.getDisplayCutout().getSafeInsetBottom());
                }
                view.setPadding(left, top, right, bottom);
            }
            return windowInsets;
        });
        setContentView(root);
        root.requestApplyInsets();
        // AndroidApplication forwards pause/resume/dispose to ProjectBlueGame on the GL thread.
    }
    @Override protected void onResume() { super.onResume(); if (platform != null) platform.resume(); }
    @Override protected void onPause() { if (platform != null) platform.pause(); super.onPause(); }
    @Override protected void onDestroy() { if (platform != null) platform.destroy(); super.onDestroy(); }
}
