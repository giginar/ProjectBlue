package com.projectblue.game.platform;

/** The SDK is the authority; an error is never interpreted as consent. */
public final class ConsentGate {
    private boolean evaluated, allowed, formOpen;
    public synchronized void begin() { evaluated = false; allowed = false; }
    public synchronized void update(boolean sdkCanRequestAds) { evaluated = true; allowed = sdkCanRequestAds; }
    public synchronized void formOpen(boolean value) { formOpen = value; }
    public synchronized boolean canRequestAds() { return evaluated && allowed && !formOpen; }
}
