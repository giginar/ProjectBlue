package com.projectblue.game.platform;

import com.projectblue.game.events.GameEvents;
import com.projectblue.game.save.Profile;
import java.util.function.LongSupplier;

/** Converts combat events into short, throttled platform feedback without owning device APIs. */
public final class CombatHaptics implements GameEvents.Listener {
    static final long KILL_COOLDOWN_NANOS=90_000_000L;
    private final PlatformService platform;
    private final Profile profile;
    private final LongSupplier clock;
    private long lastKill=Long.MIN_VALUE;
    private boolean active;
    public CombatHaptics(PlatformService platform,Profile profile) { this(platform,profile,System::nanoTime); }
    CombatHaptics(PlatformService platform,Profile profile,LongSupplier clock) {
        this.platform=platform; this.profile=profile; this.clock=clock;
    }
    public void setActive(boolean active) { this.active=active; }
    @Override public void onEvent(GameEvents.Type type,float x,float y,int value) {
        if (!active||!profile.hapticEnabled) return;
        switch (type) {
            case DRONE_DESTROYED -> {
                long now=clock.getAsLong();
                if (lastKill!=Long.MIN_VALUE&&now-lastKill<KILL_COOLDOWN_NANOS) return;
                lastKill=now; platform.haptic(value>=180?PlatformService.Haptic.HEAVY:PlatformService.Haptic.LIGHT);
            }
            case BOSS_DEFEATED -> platform.haptic(PlatformService.Haptic.BOSS);
            case PLAYER_HIT -> platform.haptic(PlatformService.Haptic.DAMAGE);
            case TURTLE_RESCUED -> platform.haptic(PlatformService.Haptic.SUCCESS);
            case SONAR_PULSE -> platform.haptic(PlatformService.Haptic.LIGHT);
            default -> { }
        }
    }
}
