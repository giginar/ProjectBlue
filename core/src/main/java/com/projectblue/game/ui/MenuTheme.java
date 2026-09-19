package com.projectblue.game.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Array;

/** Original sonar-console panels, built from pixels and the existing original font. */
public final class MenuTheme implements Disposable {
    public final Skin skin = new Skin();
    public final Drawable panel, star;
    private final Array<Pixmap> sourcePixels = new Array<>(5);
    public MenuTheme(BitmapFont font) {
        skin.add("default-font", font);
        star = star();
        panel = panel("panel", Palette.PANEL, Palette.EDGE);
        Drawable active = panel("active", Palette.EDGE, Palette.AQUA);
        Drawable pressed = panel("pressed", Palette.AQUA, Palette.AQUA);
        Drawable disabled = panel("disabled", Palette.INK, Palette.EDGE);
        skin.add("default", new Label.LabelStyle(font, Palette.TEXT));
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle(panel, pressed, active, font);
        button.fontColor = Palette.TEXT; button.downFontColor = Palette.INK;
        button.disabled = disabled; button.disabledFontColor = Palette.MUTED;
        button.over = active;
        skin.add("default", button);
        Button.ButtonStyle card = new Button.ButtonStyle(panel, active, active);
        card.over = active; card.disabled = disabled;
        skin.add("default", card);
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScroll = disabled; scroll.vScrollKnob = active;
        skin.add("default", scroll);
    }
    private Drawable star() {
        Pixmap pixels = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, b = a + Math.PI / 5;
            int radius = i % 2 == 0 ? 15 : 6, next = i % 2 == 0 ? 6 : 15;
            pixels.fillTriangle(16, 16, 16 + (int) (Math.cos(a) * radius), 16 + (int) (Math.sin(a) * radius),
                16 + (int) (Math.cos(b) * next), 16 + (int) (Math.sin(b) * next));
        }
        Texture texture = managedTexture(pixels);
        skin.add("star", texture);
        return new TextureRegionDrawable(texture);
    }
    private Drawable panel(String name, Color fill, Color border) {
        Pixmap pixmap = new Pixmap(6, 6, Pixmap.Format.RGBA8888);
        pixmap.setColor(border); pixmap.fill();
        pixmap.setColor(fill); pixmap.fillRectangle(1, 1, 4, 4);
        Texture texture = managedTexture(pixmap);
        skin.add(name, texture);
        NinePatchDrawable drawable = new NinePatchDrawable(new NinePatch(texture, 1, 1, 1, 1));
        drawable.setMinSize(4, 4);
        return drawable;
    }
    private Texture managedTexture(Pixmap pixels) {
        // Retain the small CPU source until shutdown so Android can restore a lost GL context.
        sourcePixels.add(pixels);
        return new Texture(new PixmapTextureData(pixels, pixels.getFormat(), false, false, true));
    }
    public void dispose() {
        // AssetManager owns the shared font; Skin owns only its generated textures.
        skin.remove("default-font", BitmapFont.class);
        skin.dispose();
        for (Pixmap pixels : sourcePixels) pixels.dispose();
        sourcePixels.clear();
    }
}
