package com.projectblue.game.platform;

/** Allows one in-flight SDK initialization and prevents repeats after success. */
public final class InitializationGate {
    private enum State { IDLE, STARTING, READY }
    private State state = State.IDLE;
    public synchronized boolean begin() {
        if (state != State.IDLE) return false;
        state = State.STARTING;
        return true;
    }
    public synchronized void complete(boolean success) { state = success ? State.READY : State.IDLE; }
    public synchronized boolean ready() { return state == State.READY; }
}
