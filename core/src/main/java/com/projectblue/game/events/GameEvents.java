package com.projectblue.game.events;

/** Synchronous, game-thread-only bus. Payloads and subscriptions do not allocate per event. */
public final class GameEvents {
    public enum Type { SHOT, SONAR_PULSE, DRONE_DESTROYED, PLASTIC_COLLECTED, TURTLE_RESCUED, PLAYER_HIT, SALVAGE_COLLECTED, FINISHED }
    @FunctionalInterface
    public interface Listener { void onEvent(Type type, float x, float y, int value); }
    private final Listener[] listeners = new Listener[8];
    private int count;
    public void subscribe(Listener listener) {
        for (int i = 0; i < count; i++) if (listeners[i] == listener) return;
        if (count == listeners.length) throw new IllegalStateException("Listener capacity reached");
        listeners[count++] = listener;
    }
    public void unsubscribe(Listener listener) {
        for (int i = 0; i < count; i++) {
            if (listeners[i] == listener) {
                System.arraycopy(listeners, i + 1, listeners, i, count - i - 1);
                listeners[--count] = null;
                return;
            }
        }
    }
    public void emit(Type type, float x, float y, int value) {
        for (int i = 0; i < count; i++) listeners[i].onEvent(type, x, y, value);
    }
}
