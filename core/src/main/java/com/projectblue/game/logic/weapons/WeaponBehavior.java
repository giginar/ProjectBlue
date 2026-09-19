package com.projectblue.game.logic.weapons;

import com.projectblue.game.config.ContentCatalog.WeaponDef;
import com.projectblue.game.logic.GameWorld;

/** Strategies share targeting/projectile primitives instead of branching on weapon IDs. */
@FunctionalInterface
public interface WeaponBehavior {
    void fire(GameWorld world, WeaponDef definition, int damage);
}
