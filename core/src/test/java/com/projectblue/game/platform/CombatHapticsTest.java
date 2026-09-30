package com.projectblue.game.platform;

import com.projectblue.game.events.GameEvents;
import com.projectblue.game.save.Profile;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CombatHapticsTest {
    private static final class RecordingPlatform extends NoOpPlatformService {
        final List<Haptic> calls=new ArrayList<>();
        RecordingPlatform() { super("."); }
        @Override public void haptic(Haptic kind) { calls.add(kind); }
    }
    @Test void enabledKillsThrottleBurstsAndSelectHeavyFeedback() {
        Profile profile=new Profile(); RecordingPlatform platform=new RecordingPlatform(); long[] now={1};
        CombatHaptics haptics=new CombatHaptics(platform,profile,() -> now[0]); haptics.setActive(true);
        haptics.onEvent(GameEvents.Type.DRONE_DESTROYED,0,0,100);
        now[0]+=CombatHaptics.KILL_COOLDOWN_NANOS/2;
        haptics.onEvent(GameEvents.Type.DRONE_DESTROYED,0,0,250);
        assertEquals(List.of(PlatformService.Haptic.LIGHT),platform.calls);
        now[0]+=CombatHaptics.KILL_COOLDOWN_NANOS;
        haptics.onEvent(GameEvents.Type.DRONE_DESTROYED,0,0,250);
        assertEquals(List.of(PlatformService.Haptic.LIGHT,PlatformService.Haptic.HEAVY),platform.calls);
    }
    @Test void disabledOrBackgroundStateSuppressesEveryCombatPulse() {
        Profile profile=new Profile(); RecordingPlatform platform=new RecordingPlatform();
        CombatHaptics haptics=new CombatHaptics(platform,profile,() -> 1);
        haptics.onEvent(GameEvents.Type.PLAYER_HIT,0,0,1);
        haptics.setActive(true); profile.hapticEnabled=false;
        haptics.onEvent(GameEvents.Type.DRONE_DESTROYED,0,0,100);
        haptics.onEvent(GameEvents.Type.BOSS_DEFEATED,0,0,500);
        assertTrue(platform.calls.isEmpty());
    }
    @Test void bossUsesItsOwnShortPatternSelection() {
        Profile profile=new Profile(); RecordingPlatform platform=new RecordingPlatform();
        CombatHaptics haptics=new CombatHaptics(platform,profile,() -> 1); haptics.setActive(true);
        haptics.onEvent(GameEvents.Type.BOSS_DEFEATED,0,0,500);
        assertEquals(List.of(PlatformService.Haptic.BOSS),platform.calls);
    }
    @Test void desktopNoOpAcceptsAllFeedbackKinds() {
        NoOpPlatformService desktop=new NoOpPlatformService(".");
        assertDoesNotThrow(() -> { for(PlatformService.Haptic kind:PlatformService.Haptic.values()) desktop.haptic(kind); });
    }
}
