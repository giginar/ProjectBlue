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
import com.projectblue.game.config.MissionConfig;
import com.projectblue.game.config.ContentCatalog;
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
    private final Cell<?> divisionCell, titleCell, subtitleCell, statusCell, backCell;
    private float time;
    private TextButton doubleReward, continueReward;
    protected void rewardActions(boolean failed) {
        if (!game.platform().ads().isSupported()) return;
        if (failed) {
            continueReward = button("reward-continue", "Watch ad / Continue once", () -> game.router().reward(com.projectblue.game.platform.RunRewards.Reward.CONTINUE));
            body.add(continueReward).height(84).row();
            note(t("ad.continue_help"));
        } else {
            doubleReward = button("reward-double", "Watch ad / Double this dive's salvage", () -> game.router().reward(com.projectblue.game.platform.RunRewards.Reward.DOUBLE_SALVAGE));
            body.add(doubleReward).height(84).row();
        }
        note(t("ad.help"));
    }
    private void updateRewardButton(TextButton button, com.projectblue.game.platform.RunRewards.Reward reward, String title) {
        if (button == null) return;
        var rewards = game.router().rewards();
        boolean eligible = rewards != null && rewards.eligible(reward);
        boolean ready = eligible && rewards.available(reward);
        button.setDisabled(!ready);
        button.setText(ready ? title : rewards != null && rewards.busy() ? t("ad.progress")
            : eligible ? t("ad.not_ready") : t("ad.unavailable"));
    }

    protected StageMenuScreen(ProjectBlueGame game, String title, String subtitle) {
        this.game = game; profile = game.saves().profile(); skin = game.menuTheme().skin;
        root.setFillParent(true); stage.addActor(root);
        root.add(frame).width(492).growY();
        frame.padTop(22).padBottom(18);
        Label division = label(t("division.title"), .78f, Palette.AQUA);
        divisionCell = frame.add(division).growX().minHeight(28).padBottom(14); frame.row();
        titleCell = frame.add(label(title, 1.55f, Palette.TEXT)).growX().minHeight(52).padBottom(12); frame.row();
        subtitleCell = frame.add(label(subtitle, .88f, Palette.MUTED)).growX().minHeight(48).padBottom(18); frame.row();
        body.top(); body.defaults().growX().spaceBottom(12);
        scroll = new ScrollPane(body, skin);
        scroll.setFadeScrollBars(false); scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false); scroll.setSmoothScrolling(false);
        frame.add(scroll).grow().row();
        status = label("", .74f, Palette.GOLD);
        status.setName("menu-status");
        statusCell = frame.add(status).growX().minHeight(34).padTop(8); frame.row();
        TextButton back = button("back", t("common.back"), this::back);
        backCell = frame.add(back).growX().height(84); frame.row();
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int key) {
                if (key == Input.Keys.ESCAPE || key == Input.Keys.BACK) { back(); return true; }
                return false;
            }
        });
    }
    protected Label label(String text, float scale, Color color) {
        Label label = new Label(text.toUpperCase(Locale.ROOT), new Label.LabelStyle(skin.getFont("default-font"), color));
        label.setFontScale(scale * (profile.largeUi ? 1.12f : 1)); label.setWrap(true); label.setAlignment(Align.left);
        return label;
    }
    protected String t(String key, Object... arguments) { return game.i18n().text(key, arguments); }
    protected String contentName(Enum<?> id) { return t("content." + id.name() + ".name"); }
    protected String contentDescription(Enum<?> id) { return t("content." + id.name() + ".description"); }
    protected String unlock(ContentCatalog.Unlock condition) {
        return condition.metric() == ContentCatalog.UnlockMetric.ALWAYS ? t("unlock.ALWAYS") : t("unlock." + condition.metric(), condition.target());
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
        button.setName(id); button.getLabel().setFontScale(profile.largeUi ? 1.05f : .95f); button.getLabel().setWrap(true);
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
    protected void compactLayout(boolean compact) {}
    protected void scrollToTop() { scroll.setScrollY(0); }
    public Stage stage() { return stage; }
    public void show() { Gdx.input.setInputProcessor(stage); stage.setScrollFocus(scroll); }
    public void hide() { stage.cancelTouchFocus(); Gdx.input.setInputProcessor(null); }
    public void pause() { stage.cancelTouchFocus(); }
    public void resize(int width, int height) {
        stage.cancelTouchFocus();
        game.ui().resize(width, height);
        stage.getViewport().update(width, height, true);
        float scaleX=stage.getViewport().getWorldWidth()/Math.max(1,Gdx.graphics.getWidth());
        float scaleY=stage.getViewport().getWorldHeight()/Math.max(1,Gdx.graphics.getHeight());
        float safeLeft=Gdx.graphics.getSafeInsetLeft()*scaleX, safeRight=Gdx.graphics.getSafeInsetRight()*scaleX;
        float safeTop=Gdx.graphics.getSafeInsetTop()*scaleY, safeBottom=Gdx.graphics.getSafeInsetBottom()*scaleY;
        root.pad(safeTop,safeRight,safeBottom,safeLeft);
        root.getCell(frame).width(Math.min(640, stage.getViewport().getWorldWidth() - 48 - safeLeft - safeRight));
        boolean compact = stage.getViewport().getWorldHeight() < 1050;
        frame.padTop(compact ? 12 : 22).padBottom(compact ? 10 : 18);
        divisionCell.minHeight(compact ? 22 : 28).padBottom(compact ? 8 : 14);
        titleCell.minHeight(compact ? 46 : 52).padBottom(compact ? 7 : 12);
        subtitleCell.minHeight(compact ? 40 : 48).padBottom(compact ? 10 : 18);
        statusCell.minHeight(compact ? 28 : 34).padTop(compact ? 4 : 8);
        backCell.height(compact ? 72 : 84);
        for (Cell<?> cell : body.getCells()) cell.spaceBottom(compact ? 8 : 12);
        compactLayout(compact);
        root.invalidateHierarchy();
    }
    public void render(float delta) {
        updateRewardButton(doubleReward, com.projectblue.game.platform.RunRewards.Reward.DOUBLE_SALVAGE, "WATCH AD / DOUBLE SALVAGE");
        updateRewardButton(continueReward, com.projectblue.game.platform.RunRewards.Reward.CONTINUE, "WATCH AD / CONTINUE ONCE");
        if (!profile.reducedMotion) time += Math.min(Math.max(delta, 0), .1f);
        game.ocean().backdrop(time, backdropRestoration(), backdropType(), profile.reducedMotion);
        if (game.saves().writeFailed()) status.setText(t("common.save_failed"));
        else if (game.saves().recoveredBackup()) status.setText(t("common.backup_restored"));
        else if (game.saves().recovered()) status.setText(t("common.defaults_restored"));
        else status.setText(t("common.salvage", profile.totalSalvage, profile.completedRuns));
        stage.getViewport().apply();
        stage.act(Math.min(Math.max(delta, 0), .1f)); stage.draw();
    }
    public void dispose() { stage.dispose(); }
    protected float backdropRestoration() { return .45f; }
    protected MissionConfig.MissionType backdropType() { return MissionConfig.MissionType.BLUE_COAST; }
}
