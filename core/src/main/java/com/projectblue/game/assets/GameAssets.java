package com.projectblue.game.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

public final class GameAssets implements Disposable {
    public static final String FONT = "fonts/blue.fnt", PULSE = "audio/pulse.wav";
    public static final String COLLECT = "audio/collect.wav", OCEAN = "audio/ocean.wav";
    private final AssetManager manager = new AssetManager();
    public void queue() {
        manager.load(FONT, BitmapFont.class);
        manager.load(PULSE, Sound.class);
        manager.load(COLLECT, Sound.class);
        manager.load(OCEAN, Music.class);
    }
    public boolean update() { return manager.update(); }
    public float progress() { return manager.getProgress(); }
    public BitmapFont font() { return manager.get(FONT, BitmapFont.class); }
    public Sound pulse() { return manager.get(PULSE, Sound.class); }
    public Sound collect() { return manager.get(COLLECT, Sound.class); }
    public Music ocean() { return manager.get(OCEAN, Music.class); }
    public void dispose() { manager.dispose(); }
}

