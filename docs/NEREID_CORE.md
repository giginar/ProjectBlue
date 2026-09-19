# Sector 10: NEREID Core

NEREID Core is the campaign finale in the ocean-floor headquarters of NEREID Industries. The authored timeline moves from the exterior alarm grid through defense formations and selected net, oil, sonar, ruin, and recovery mechanics. A Core Sentinel protects the approach. Two continuous-interaction power cores must be disabled before the shared `GameWorld` starts the final boss.

The procedural palette begins with metallic black structures and red alarm lights. Restoration changes the facility toward blue and living green. No external art or audio assets are added.

## Leviathan Core

`LeviathanCore` is a pure Java state machine with four numbered phases:

1. Archived NEREID weapon patterns rotate through aimed fans, crossing nets, oil surges, and sonar rings.
2. Two shield generators block core damage while lane and contamination patterns continue.
3. Captured cleanup, sonar, and rescue systems must all be used. The mission spawns persistent contamination and wildlife objectives that the starting TIDE / Kaia / Pulse Cannon loadout can complete.
4. The central core opens. Destroying it starts a countdown; the player must reach and hold the upper exit while telegraphed collapse hazards fall.

Every emitted boss attack first occupies a warning window. HUD text names the pending pattern and the renderer marks the boss or collapse target. Normal, Hard, Expert, and Abyss change cadence, projectile count, pattern selection, simultaneous hazard count, phase-three requirements, warning duration, and escape time in addition to health.

## Finale and persistence

Completing the escape starts the habitat recovery transition and records sector 10 through the existing atomic profile save. Campaign completion is derived from the saved Normal completion flag for the stable sector 10 record; the profile schema remains version 5.

The final results scene lists all ten regions with restored colors, the wildlife represented by completed mission records, total stars, average cleanup and rescue, achievement count, and the `Guardian of the Blue` unlock. It links to Credits, NEREID Core replay, and the full level chart. After reload, Main Menu exposes Final Results and every unlocked sector remains replayable.

## Manual checks

1. Enter with the base loadout and verify both power cores can be disabled without upgrades.
2. Check that the Core Sentinel appears before headquarters entry and that each Leviathan Core attack is named before it fires.
3. In phase three, clean contamination, rescue wildlife, and spend sonar pulses. Confirm no equipment choice is required.
4. Destroy the exposed core, move into the upper exit, and hold until escape completes. Repeat without reaching the exit and confirm the dive fails when the countdown reaches zero.
5. Compare Normal, Hard, Expert, and Abyss for different pattern sets, hazard combinations, restoration requirements, warning windows, and escape times.
6. Complete the finale, open Credits, return to the level chart, replay an earlier sector, exit, and reload. Confirm the final achievement and all records survive.
