package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;

/** Plain-language disclosure for the current offline build. */
public final class PrivacyScreen extends StageMenuScreen {
    public PrivacyScreen(ProjectBlueGame game) {
        super(game, "Privacy", "What this build stores and shares");
        note("Project Blue stores progress and settings only on this device.");
        if (game.platform().ads().isSupported()) {
            note("This Android build uses Google AdMob and Google's consent service. When ads are permitted, Google may collect device identifiers, approximate location from IP address, ad interactions and diagnostics.");
            note("Ads are optional for rewards. Occasional ads may appear when leaving successful results. Internet is never required to play.");
            note("Privacy Options appears in Settings when Google's consent service requires it. You can revisit your choices there.");
        } else note("Advertising requests are disabled in this build. There is no account sign-in, analytics upload or cloud save.");
        note("Haptic feedback uses the device vibration feature when enabled. It does not collect sensor data.");
        note("Reset profile is available only in development builds. Uninstalling the app may remove local progress.");
        note("Local progress stays on your device. Advertising privacy choices are managed separately by Google's consent service when enabled.");
    }
}
