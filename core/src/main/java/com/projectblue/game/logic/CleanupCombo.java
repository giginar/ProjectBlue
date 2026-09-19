package com.projectblue.game.logic;

/** Forgiving cleanup chain: missed windows reduce one step at a time instead of clearing the chain. */
public final class CleanupCombo {
    private final float window,decay;
    private final int maximum;
    private int value,best;
    private float timer;

    public CleanupCombo(float window,float decay,int maximum) {
        if (!Float.isFinite(window) || !Float.isFinite(decay) || window<=0 || decay<=0 || maximum<2)
            throw new IllegalArgumentException("Invalid cleanup combo tuning");
        this.window=window; this.decay=decay; this.maximum=maximum;
    }
    public int collect() {
        value=Math.min(maximum,value+1); best=Math.max(best,value); timer=window;
        return value;
    }
    public void update(float dt) {
        if (dt<=0 || value<=0) return;
        timer-=dt;
        while (timer<=0 && value>0) { value--; timer+=decay; }
    }
    public int value() { return value; }
    public int best() { return best; }
    public float remaining() { return Math.max(0,timer); }
    public float window() { return window; }
}
