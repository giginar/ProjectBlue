package com.projectblue.game.ui;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import static com.projectblue.game.config.GameConfig.*;

/** Shared GPU resources are owned by ProjectBlueGame, never by individual screens. */
public final class UiPainter implements Disposable {
    public final FitViewport viewport = new FitViewport(WIDTH, HEIGHT);
    public final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final GlyphLayout layout = new GlyphLayout();
    private BitmapFont font;
    public void setFont(BitmapFont font) { this.font = font; }
    public void resize(int width, int height) { viewport.update(width, height, true); }
    public void beginShapes() {
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
    public void endShapes() { shapes.end(); }
    public void rect(float x, float y, float w, float h, Color color) {
        shapes.setColor(color); shapes.rect(x, y, w, h);
    }
    public void bar(float x, float y, float w, float h, float fraction, Color color) {
        rect(x, y, w, h, Palette.EDGE);
        rect(x, y, w * Math.max(0, Math.min(1, fraction)), h, color);
    }
    public void button(float x, float y, float w, float h, boolean primary) {
        rect(x, y, w, h, primary ? Palette.AQUA : Palette.EDGE);
        if (!primary) rect(x + 2, y + 2, w - 4, h - 4, Palette.PANEL);
    }
    public void beginText() {
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
    }
    public void text(CharSequence value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale);
        layout.setText(font, value, color, 0, com.badlogic.gdx.utils.Align.left, false);
        font.draw(batch, layout, x, y);
    }
    public void centered(CharSequence value, float y, float scale, Color color) {
        font.getData().setScale(scale);
        layout.setText(font, value, color, 0, com.badlogic.gdx.utils.Align.left, false);
        font.draw(batch, layout, (WIDTH - layout.width) / 2, y);
    }
    public void endText() { batch.end(); }
    public void dispose() { shapes.dispose(); batch.dispose(); }
}
