package com.projectblue.game.input;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public final class MenuInput extends InputAdapter {
    @FunctionalInterface public interface Click { void at(float x, float y); }
    private final Viewport viewport;
    private final Click click;
    private final Runnable back;
    private final Vector2 point = new Vector2();
    private int pointer = -1;
    private int downX, downY;
    public MenuInput(Viewport viewport, Click click, Runnable back) {
        this.viewport = viewport; this.click = click; this.back = back;
    }
    public boolean touchDown(int x, int y, int id, int button) {
        if (pointer != -1 || button != Input.Buttons.LEFT) return false;
        pointer = id; downX = x; downY = y; return true;
    }
    public boolean touchUp(int x, int y, int id, int button) {
        if (id != pointer) return false;
        pointer = -1;
        viewport.unproject(point.set(x, y));
        float scale = viewport.getWorldWidth() / Math.max(1, viewport.getScreenWidth());
        if (Math.abs(x - downX) * scale < 24 && Math.abs(y - downY) * scale < 24) click.at(point.x, point.y);
        return true;
    }
    public boolean touchCancelled(int x, int y, int id, int button) { pointer = -1; return true; }
    public boolean keyDown(int key) {
        if (key == Input.Keys.BACK || key == Input.Keys.ESCAPE) { back.run(); return true; }
        return false;
    }
    public void reset() { pointer = -1; }
}

