package com.projectblue.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.ui.Palette;
import com.projectblue.game.save.Profile;
import java.util.Locale;

/** Responsive menus use a separate viewport; the original gameplay coordinates stay intact. */
public abstract class StageMenuScreen extends ScreenAdapter {
    protected final ProjectBlueGame game;
    protected final Profile profile;
    protected final Skin skin;
    protected final Stage stage = new Stage(new ExtendViewport(540, 720));
    protected final Table body = new Table();
    private final Table root = new Table(), frame = new Table();
    private final Label status;
    private final ScrollPane scroll;
    private float time;

    protected StageMenuScreen(ProjectBlueGame game, String title, String subtitle) {
        this.game = game; profile = game.saves().profile(); skin = game.menuTheme().skin;
        root.setFillParent(true); stage.addActor(root);
        root.add(frame).width(492).growY();
        frame.padTop(22).padBottom(18);
        Label division = label("OCEAN RECOVERY DIVISION", .78f, Palette.AQUA);
        frame.add(division).growX().padBottom(14).row();
        frame.add(label(title, 1.55f, Palette.TEXT)).growX().padBottom(12).row();
        frame.add(label(subtitle, .88f, Palette.MUTED)).growX().padBottom(18).row();
        body.top(); body.defaults().growX().spaceBottom(12);
        scroll = new ScrollPane(body, skin);
        scroll.setFadeScrollBars(false); scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false); scroll.setSmoothScrolling(false);
        frame.add(scroll).grow().row();
        status = label("", .74f, Palette.GOLD);
        frame.add(status).growX().minHeight(34).padTop(8).row();
        TextButton back = button("back", "Back", this::back);
        frame.add(back).growX().height(84).row();
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int key) {
                if (key == Input.Keys.ESCAPE || key == Input.Keys.BACK) { back(); return true; }
                return false;
            }
        });
    }
    protected Label label(String text, float scale, Color color) {
        Label label = new Label(text.toUpperCase(Locale.ROOT), new Label.LabelStyle(skin.getFont("default-font"), color));
        label.setFontScale(scale); label.setWrap(true); label.setAlignment(Align.left);
        return label;
    }
    protected void note(String text) { body.add(label(text, .92f, Palette.MUTED)).growX().padBottom(16).row(); }
    protected Table rating(int count) {
        Table stars = new Table(); stars.left();
        for (int i = 0; i < 3; i++) {
            Image star = new Image(game.menuTheme().star);
            star.setColor(i < count ? Palette.GOLD : Palette.MUTED);
            if (i >= count) star.getColor().a = .35f;
            stars.add(star).size(32).padRight(12);
        }
        stars.add(label(count + " / 3", .95f, Palette.GOLD)).width(110);
        return stars;
    }
    protected Table panel() {
        Table panel = new Table(); panel.setBackground(game.menuTheme().panel); panel.pad(20);
        panel.defaults().growX().spaceBottom(12);
        body.add(panel).growX().row(); return panel;
    }
    protected TextButton button(String id, String text, Runnable action) {
        TextButton button = new TextButton(text.toUpperCase(Locale.ROOT), skin);
        button.setName(id); button.getLabel().setFontScale(.95f); button.getLabel().setWrap(true);
        button.setProgrammaticChangeEvents(false);
        button.pad(14);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (!button.isDisabled()) { action.run(); button.setChecked(false); }
            }
        });
        return button;
    }
    protected void action(String id, String text, Runnable action) { body.add(button(id, text, action)).height(84).row(); }
    protected void route(String id, String text, ScreenRouter.Route route) { action(id, text, () -> game.router().request(route)); }
    protected void save() { game.saves().save(); }
    protected void back() { game.router().request(ScreenRouter.Route.MENU); }
    protected void scrollToTop() { scroll.setScrollY(0); }
    public Stage stage() { return stage; }
    public void show() { Gdx.input.setInputProcessor(stage); stage.setScrollFocus(scroll); }
    public void hide() { stage.cancelTouchFocus(); Gdx.input.setInputProcessor(null); }
    public void pause() { stage.cancelTouchFocus(); }
    public void resize(int width, int height) {
        stage.cancelTouchFocus();
        game.ui().resize(width, height);
        stage.getViewport().update(width, height, true);
        root.getCell(frame).width(Math.min(640, stage.getViewport().getWorldWidth() - 48));
    }
    public void render(float delta) {
        if (!profile.reducedMotion) time += Math.min(Math.max(delta, 0), .1f);
        game.ocean().backdrop(time, .45f);
        if (game.saves().writeFailed()) status.setText("SAVE FAILED / RETRY IN SETTINGS");
        else if (game.saves().recoveredBackup()) status.setText("PROFILE RESTORED FROM BACKUP");
        else if (game.saves().recovered()) status.setText("DAMAGED PROFILE / DEFAULTS RESTORED");
        else status.setText("SALVAGE " + profile.totalSalvage + "  /  DIVES " + profile.completedRuns);
        stage.getViewport().apply();
        stage.act(Math.min(Math.max(delta, 0), .1f)); stage.draw();
    }
    public void dispose() { stage.dispose(); }
}
