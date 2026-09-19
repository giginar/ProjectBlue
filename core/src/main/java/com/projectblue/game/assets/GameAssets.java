package com.projectblue.game.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

public final class GameAssets implements Disposable {
    public static final String FONT = "fonts/blue.fnt", PULSE = "audio/pulse.wav";
    public static final String COLLECT = "audio/collect.wav", OCEAN = "audio/ocean.wav";
    private final AssetManager manager = new AssetManager();
    private BitmapFont fallbackFont;
    public GameAssets() {
        manager.setErrorListener((asset, error) -> Gdx.app.error("ASSETS", "Using fallback for " + asset.fileName, error));
    }
    public void queue() {
        manager.load(FONT, BitmapFont.class);
        manager.load(PULSE, Sound.class);
        manager.load(COLLECT, Sound.class);
        manager.load(OCEAN, Music.class);
    }
    public boolean update() { return manager.update(); }
    public float progress() { return manager.getProgress(); }
    public BitmapFont font() {
        if (manager.isLoaded(FONT, BitmapFont.class)) return manager.get(FONT, BitmapFont.class);
        if (fallbackFont == null) fallbackFont = new BitmapFont();
        return fallbackFont;
    }
    public Sound pulse() { return manager.isLoaded(PULSE, Sound.class) ? manager.get(PULSE, Sound.class) : null; }
    public Sound collect() { return manager.isLoaded(COLLECT, Sound.class) ? manager.get(COLLECT, Sound.class) : null; }
    public Music ocean() { return manager.isLoaded(OCEAN, Music.class) ? manager.get(OCEAN, Music.class) : null; }
    public void dispose() { manager.dispose(); if (fallbackFont != null) fallbackFont.dispose(); }
}
