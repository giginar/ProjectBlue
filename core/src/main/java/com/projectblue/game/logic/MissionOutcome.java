package com.projectblue.game.logic;

/** Created once at the terminal state; includes actual encounter denominators. */
public record MissionOutcome(int kills,int enemiesEncountered,int plastic,int cleaned,int rescued,
    int salvage,int health,int damageTaken,int coralDamage,int combatScore,float restoration) {}
