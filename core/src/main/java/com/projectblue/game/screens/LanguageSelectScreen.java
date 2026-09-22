package com.projectblue.game.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.i18n.GameLanguage;

/** Explicit first-launch language choice. Device locale only controls initial focus. */
public final class LanguageSelectScreen extends StageMenuScreen {
    private GameLanguage selected = GameLanguage.suggested(java.util.Locale.getDefault());
    public LanguageSelectScreen(ProjectBlueGame game) {
        super(game, "CHOOSE LANGUAGE / DİL SEÇİN", "Select and confirm / Seçip onaylayın");
        for (GameLanguage language : GameLanguage.values())
            action("language-" + language.id(), language.displayName(), () -> confirm(language));
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.UP || keycode == Input.Keys.DOWN) {
                    selected = selected == GameLanguage.ENGLISH ? GameLanguage.TURKISH : GameLanguage.ENGLISH; refresh(); return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.BUTTON_A) { confirm(selected); return true; }
                return false;
            }
        });
        refresh();
        stage.getRoot().findActor("back").setVisible(false);
        stage.getRoot().findActor("menu-status").setVisible(false);
    }
    private void refresh() {
        for (GameLanguage language : GameLanguage.values()) {
            TextButton button = stage.getRoot().findActor("language-" + language.id());
            button.setChecked(language == selected);
        }
        stage.setKeyboardFocus(stage.getRoot().findActor("language-" + selected.id()));
    }
    private void confirm(GameLanguage language) { game.chooseLanguage(language); game.router().request(ScreenRouter.Route.MENU); }
    @Override protected void back() { }
}
