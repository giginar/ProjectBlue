package com.projectblue.game.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.projectblue.game.logic.GameWorld;
import static com.projectblue.game.config.GameConfig.*;

/** One captured pointer; mouse and touch share relative drag semantics. */
public final class PointerInput extends InputAdapter implements PlayerInput {
    private final Viewport viewport;
    private final GameWorld world;
    private final Runnable pause;
    private final Vector2 point = new Vector2();
    private int pointer = -1;
    private float originX, originY, startX, startY, targetX, targetY;
    public PointerInput(Viewport viewport, GameWorld world, Runnable pause) {
        this.viewport = viewport; this.world = world; this.pause = pause;
    }
    private void project(int x, int y) { viewport.unproject(point.set(x, y)); }
    public boolean touchDown(int x, int y, int id, int button) {
        if (button != Input.Buttons.LEFT || pointer != -1) return false;
        project(x, y);
        if (point.x < 0 || point.x > WIDTH || point.y < 0 || point.y > HEIGHT) return false;
        if (point.x >= 444 && point.y >= 864) { pause.run(); return true; }
        if (world.hasSonar() && point.x>=408 && point.x<=516 && point.y>=650 && point.y<=706) {
            world.activateSonar(); return true;
        }
        if (point.y >= 838) return false;
        pointer = id; originX = point.x; originY = point.y;
        startX = targetX = world.player.x; startY = targetY = world.player.y;
        return true;
    }
    public boolean touchDragged(int x, int y, int id) {
        if (id != pointer) return false;
        project(x, y);
        targetX = startX + point.x - originX;
        targetY = startY + point.y - originY;
        return true;
    }
    public boolean touchUp(int x, int y, int id, int button) {
        if (id != pointer) return false;
        reset(); return true;
    }
    public boolean touchCancelled(int x, int y, int id, int button) { return touchUp(x, y, id, button); }
    public boolean keyDown(int key) {
        if (key == Input.Keys.ESCAPE || key == Input.Keys.BACK || key == Input.Keys.P) { pause.run(); return true; }
        if ((key==Input.Keys.SPACE || key==Input.Keys.S) && world.hasSonar()) { world.activateSonar(); return true; }
        return false;
    }
    public boolean moving() { return pointer != -1; }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public void reset() { pointer = -1; }
}
