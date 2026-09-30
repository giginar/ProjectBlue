package com.projectblue.game.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.save.LevelRecord;
import com.projectblue.game.ui.Palette;

/** One centered sector card with discrete touch and controller paging. */
public final class LevelSelectScreen extends StageMenuScreen {
    private final Difficulty[] choices=new Difficulty[CampaignConfig.LEVEL_COUNT];
    private int selectedLevel;
    private float touchStartY;
    private boolean trackingSwipe;
    public LevelSelectScreen(ProjectBlueGame game) {
        super(game,game.i18n().text("levels.title"),game.i18n().text("levels.subtitle"));
        java.util.Arrays.fill(choices,Difficulty.NORMAL);
        selectedLevel=LevelSelectPolicy.move(game.router().selectedLevel(),0);
        choices[selectedLevel-1]=game.router().selectedDifficulty();
        showCard(false);
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event,int keycode) {
                if (keycode==Input.Keys.UP) { select(1); return true; }
                if (keycode==Input.Keys.DOWN) { select(-1); return true; }
                if (keycode==Input.Keys.ENTER||keycode==Input.Keys.SPACE||keycode==Input.Keys.BUTTON_A) return start();
                return false;
            }
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,int button) {
                if (pointer!=0) return false; touchStartY=y; trackingSwipe=true; return false;
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,int button) {
                if (!trackingSwipe||pointer!=0) return;
                trackingSwipe=false; int target=LevelSelectPolicy.afterSwipe(selectedLevel,y-touchStartY);
                if (target!=selectedLevel) { selectedLevel=target; remember(); showCard(true); }
            }
        });
    }
    private void select(int direction) {
        int target=LevelSelectPolicy.move(selectedLevel,direction);
        if(target==selectedLevel) return;
        selectedLevel=target; remember(); showCard(true);
    }
    private void remember() { game.router().rememberLevelSelection(selectedLevel); }
    private void showCard(boolean animate) {
        body.clearChildren(); note(t("levels.help"));
        body.add(label(t("levels.page",selectedLevel,CampaignConfig.LEVEL_COUNT),.82f,Palette.AQUA)).row();
        CampaignConfig.Level level=CampaignConfig.DEFAULT.level(selectedLevel);
        LevelRecord record=profile.level(selectedLevel);
        boolean available=CampaignConfig.isAvailable(selectedLevel);
        boolean unlocked=LevelSelectPolicy.canStart(profile,selectedLevel,Difficulty.NORMAL);
        Table card=panel(); card.setName("level-card-"+selectedLevel);
        String state=!available?t("levels.coming"):!unlocked?t("levels.locked"):"";
        TextButton dive=button("level-"+selectedLevel,t("levels.card",String.format(java.util.Locale.ROOT,"%02d",selectedLevel),levelName(selectedLevel),state),this::start);
        dive.setDisabled(!unlocked); card.add(dive).height(84).row();
        String status=!available?t("levels.planned"):!unlocked?t("common.locked"):record.bestStars>0?t("levels.restored"):t("levels.open");
        String difficultyStatus=record.bestDifficulty()==null?t("levels.normal_ready"):t("levels.difficulty_cleared",difficulty(record.bestDifficulty()));
        card.add(label(t("levels.status",status,difficultyStatus),.78f,unlocked?Palette.AQUA:Palette.MUTED)).row();
        card.add(label(region(selectedLevel),1,Palette.AQUA)).row();
        card.add(label(t("level."+selectedLevel+".description"),.82f,Palette.TEXT)).row();
        card.add(rating(record.bestStars)).height(36).row();
        card.add(label(t("levels.best_score",record.bestScore),.92f,Palette.GOLD)).row();
        card.add(label(t("levels.cleared",completed(record)),.82f,Palette.TEXT)).row();
        card.add(label(t("levels.performance",Math.round(record.bestCleanup),Math.round(record.bestRescue)),.82f,Palette.MUTED)).row();
        if(!unlocked) card.add(label(t("levels.requirement",selectedLevel-1),.82f,Palette.MUTED)).row();
        else {
            card.add(label(t("levels.tap"),.78f,Palette.GOLD)).row();
            TextButton difficultyButton=button("difficulty-"+selectedLevel,difficulty(choices[selectedLevel-1]),() -> {
                choices[selectedLevel-1]=nextDifficulty(record,choices[selectedLevel-1]); showCard(false);
            });
            card.add(difficultyButton).height(62).row();
        }
        if(animate&&!profile.reducedMotion) card.addAction(Actions.sequence(Actions.alpha(.55f),Actions.fadeIn(.16f)));
        stage.setKeyboardFocus(dive); scrollToTop();
    }
    private boolean start() {
        return LevelSelectPolicy.canStart(profile,selectedLevel,choices[selectedLevel-1])
            &&game.router().requestDive(selectedLevel,choices[selectedLevel-1]);
    }
    private Difficulty nextDifficulty(LevelRecord record,Difficulty current) {
        Difficulty[] all=Difficulty.values();
        for(int offset=1;offset<=all.length;offset++) {
            Difficulty candidate=all[(current.ordinal()+offset)%all.length];
            if(record.canPlay(candidate)) return candidate;
        }
        return Difficulty.NORMAL;
    }
    private String completed(LevelRecord record) {
        StringBuilder text=new StringBuilder();
        for(Difficulty d:Difficulty.values()) if(record.completed(d)) { if(text.length()>0) text.append(" / "); text.append(difficulty(d)); }
        return text.length()==0?t("common.none"):text.toString();
    }
    private String difficulty(Difficulty value) { return t("difficulty."+value.name()); }
    private String levelName(int id) { return t("level."+id+".name"); }
    private String region(int id) { return t("level."+id+".region"); }
}
