package com.projectblue.game.input;
public interface PlayerInput {
    boolean moving();
    float targetX();
    float targetY();
    void reset();
}

