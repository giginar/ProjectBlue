package com.projectblue.game.screens;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.input.MenuInput;
import com.projectblue.game.ui.UiPainter;

abstract class BaseMenuScreen extends ScreenAdapter {
    protected final ProjectBlueGame game;
    protected final UiPainter ui;
    private final MenuInput input;
    protected float time;
    protected BaseMenuScreen(ProjectBlueGame game) {
        this.game = game; ui = game.ui();
        input = new MenuInput(ui.viewport, this::click, this::back);
    }
    protected abstract void click(float x, float y);
    protected void back() { game.router().request(ScreenRouter.Route.MENU); }
    protected boolean inside(float x, float y, float left, float bottom, float width, float height) {
        return x >= left && x <= left + width && y >= bottom && y <= bottom + height;
    }
    public void show() { input.reset(); Gdx.input.setInputProcessor(input); }
    public void hide() { input.reset(); Gdx.input.setInputProcessor(null); }
    public void resize(int width, int height) { ui.resize(width, height); }
    public void pause() { input.reset(); }
}

