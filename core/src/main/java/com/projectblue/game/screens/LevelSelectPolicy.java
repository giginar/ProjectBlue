package com.projectblue.game.screens;

import com.projectblue.game.config.CampaignConfig;
import com.projectblue.game.config.Difficulty;
import com.projectblue.game.save.Profile;

/** Pure interaction policy shared by card UI and focused tests. */
public final class LevelSelectPolicy {
    public enum CardAction { START, DIFFICULTY, NONE }
    public static final float SWIPE_THRESHOLD=54;
    private LevelSelectPolicy() {}
    public static boolean canStart(Profile profile, int levelId, Difficulty difficulty) {
        return profile != null && difficulty != null && CampaignConfig.isAvailable(levelId) && profile.canPlay(levelId, difficulty);
    }
    public static int move(int current,int direction) {
        return Math.max(1,Math.min(CampaignConfig.LEVEL_COUNT,current+Integer.signum(direction)));
    }
    public static int afterSwipe(int current,float verticalDistance) {
        if (!Float.isFinite(verticalDistance)||Math.abs(verticalDistance)<SWIPE_THRESHOLD) return current;
        return move(current,verticalDistance>0?1:-1);
    }
    public static Difficulty changeDifficulty(Profile profile,int levelId,Difficulty current,int direction) {
        if (profile==null||current==null||direction==0) return current;
        int step=Integer.signum(direction),index=current.ordinal()+step;
        while(index>=0&&index<Difficulty.values().length) {
            Difficulty candidate=Difficulty.values()[index];
            if(profile.canPlay(levelId,candidate)) return candidate;
            index+=step;
        }
        return current;
    }
    public static CardAction cardAction(String actorName,boolean unlocked) {
        if(!unlocked) return CardAction.NONE;
        return actorName!=null&&actorName.startsWith("difficulty-")?CardAction.DIFFICULTY:CardAction.START;
    }
}
