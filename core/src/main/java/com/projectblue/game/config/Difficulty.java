package com.projectblue.game.config;

public enum Difficulty {
    NORMAL, HARD, EXPERT, ABYSS;

    public int bit() { return 1 << ordinal(); }
}
