package com.projectblue.game.logic;

import java.util.Random;

@FunctionalInterface
public interface RandomProvider {
    float nextFloat();
    static RandomProvider seeded(long seed) {
        Random random = new Random(seed);
        return random::nextFloat;
    }
}

