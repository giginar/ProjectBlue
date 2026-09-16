package com.projectblue.game.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.screens.*;
import com.projectblue.game.logic.GameWorld;
import static com.projectblue.game.config.GameConfig.*;

/** Optional real OpenGL integration check. Uses an isolated profile and exits on completion. */
final class DesktopSmokeGame extends ProjectBlueGame {
    private int frame, stage, resultFrames;
    private float pausedElapsed, initialX;
    private final Vector2 point = new Vector2();
    DesktopSmokeGame() { super(new NoOpPlatformService(GdxPath.profile())); }
    private static final class GdxPath {
        static String profile() { return System.getProperty("user.dir") + "/build/smoke/profile"; }
    }
    @Override public void render() {
        super.render();
        frame++;
        if (frame > 1800) throw new IllegalStateException("Desktop smoke timed out");
        switch (stage) {
            case 0 -> {
                if (getScreen() instanceof MainMenuScreen && frame > 30) {
                    capture("01-menu"); click(270, 264); stage++;
                }
            }
            case 1 -> {
                require(getScreen() instanceof GameScreen, "menu -> gameplay");
                initialX = router().activeRun().world().player.x;
                pointerDown(270,170);
                pointerDrag(420,370);
                stage++;
            }
            case 2 -> {
                if (router().activeRun().world().player.x > initialX + 90) {
                    pointerUp(420,370); capture("02-gameplay");
                    click(482,903); stage++;
                }
            }
            case 3 -> {
                require(getScreen() instanceof PauseScreen, "pause button");
                pausedElapsed = router().activeRun().world().elapsed();
                capture("03-paused"); stage++; frame=100;
            }
            case 4 -> {
                require(router().activeRun().world().elapsed() == pausedElapsed, "pause freezes simulation");
                if (frame > 115) { click(270,493); stage++; }
            }
            case 5 -> {
                require(getScreen() instanceof GameScreen, "resume button");
                pause(); stage++;
            }
            case 6 -> {
                require(getScreen() instanceof PauseScreen, "platform lifecycle suspends gameplay");
                resume();
                require(getScreen() instanceof PauseScreen, "lifecycle resume waits for player");
                click(270,493); stage++;
            }
            case 7 -> {
                require(getScreen() instanceof GameScreen, "lifecycle continue");
                Gdx.graphics.setWindowedMode(700,600); stage++; frame=200;
            }
            case 8 -> {
                if (frame > 215) {
                    require(ui().viewport.getWorldWidth()==WIDTH && ui().viewport.getWorldHeight()==HEIGHT, "fixed logical world");
                    require(ui().viewport.getScreenX()>0, "wide screen uses safe letterboxing");
                    float before=router().activeRun().world().player.x;
                    Gdx.input.getInputProcessor().touchDown(0,300,0,Input.Buttons.LEFT);
                    Gdx.input.getInputProcessor().touchDragged(690,300,0);
                    getScreen().render(STEP);
                    Gdx.input.getInputProcessor().touchUp(690,300,0,Input.Buttons.LEFT);
                    require(before==router().activeRun().world().player.x,"letterbox cannot steer");
                    capture("04-wide"); Gdx.graphics.setWindowedMode(486,864); stage++;
                }
            }
            case 9 -> {
                GameWorld world=router().activeRun().world();
                for(int i=0;i<90 && !world.finished();i++) {
                    float x=WIDTH/2f+(float)Math.sin(world.elapsed()*1.5f)*190;
                    world.update(STEP,true,x,PLAYER_START_Y);
                }
                if (world.elapsed()>35 && world.elapsed()<37) capture("05-active-reef");
                if(world.finished()) {
                    require(world.result().completed,"three-minute survival");
                    require(world.elapsed()==LEVEL_SECONDS,"180-second level");
                    stage++;
                }
            }
            case 10 -> {
                require(getScreen() instanceof ResultScreen,"results transition");
                if (++resultFrames < 4) return; // A new screen renders on the next frame.
                capture("06-result");
                require(saves().profile().completedRuns>0,"result recorded");
                click(270,217); stage++;
            }
            case 11 -> {
                require(getScreen() instanceof GameScreen,"replay creates fresh run");
                require(router().activeRun().world().elapsed()<1,"replay resets clock");
                require(router().activeRun().world().player.health==PLAYER_HEALTH,"replay resets health");
                Gdx.app.log("SMOKE","PASS: boot, menu, drag, pause, lifecycle, viewport, 180s result, save, replay");
                Gdx.app.exit(); stage++;
            }
            default -> { }
        }
    }
    private void capture(String name) {
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(Gdx.files.local("build/smoke/"+name+".png"),pixels,-1,true);
        pixels.dispose();
    }
    private void project(float x,float y) {
        ui().viewport.project(point.set(x,y));
        point.y=Gdx.graphics.getHeight()-point.y;
    }
    private void pointerDown(float x,float y) { project(x,y); Gdx.input.getInputProcessor().touchDown((int)point.x,(int)point.y,0,Input.Buttons.LEFT); }
    private void pointerDrag(float x,float y) { project(x,y); Gdx.input.getInputProcessor().touchDragged((int)point.x,(int)point.y,0); }
    private void pointerUp(float x,float y) { project(x,y); Gdx.input.getInputProcessor().touchUp((int)point.x,(int)point.y,0,Input.Buttons.LEFT); }
    private void click(float x,float y) { pointerDown(x,y); pointerUp(x,y); }
    private void require(boolean condition,String check) { if(!condition)throw new IllegalStateException("Smoke failed: "+check); }
}
