# Sectors 8 and 9

## Abyss Mine

Abyss Mine is sector 8. The mission enters NEREID Industries' deep-sea mining facility and uses black, dark purple, metallic gray, and bioluminescent blue procedural colors.

The reusable `PressureSystem` raises pressure gradually, starts warning before the damage threshold, and recovers inside authored blue safe zones. Purple pressure fields and moving drill arms display an activation countdown before they become dangerous. The existing sonar resource reveals concealed mining routes; energy stations use the continuous cleanup interaction. All pressure, warning, sonar, and encounter values are authored in `config/abyss-mine.json`.

The enemy set is DeepMiner, PressureDrone, RailTurret, and AbyssGuardian. The Harvester progresses through moving drill arms and debris, powered armor supplied by two energy stations, and an exposed core with accelerated high-pressure attacks.

## Plastic Vortex

Plastic Vortex is sector 9. The mission moves through a rotating ocean debris gyre using dirty gray, varied plastic colors, and a dark-blue vortex center.

The fixed-step `VortexSystem` applies deterministic directional force to the player, enemies, projectiles, salvage, wildlife, and cleanup objects. Current strength, direction period, trash-cluster rotation, combo windows, gradual combo decay, and escape force are authored in `config/plastic-vortex.json`. Missing a cleanup window removes one combo step at a time. Defeating the boss starts a timed current-assisted escape from the collapsing center.

The enemy set is VortexDrone, TrashSwarm, MagneticCollector, and CurrentDisruptor. Recycler Leviathan begins behind recycled plastic armor, reverses current directions after the armor breaks, then requires collected debris to be returned before its core opens.

## Difficulty, performance, and progression

Normal, Hard, Expert, and Abyss retain the campaign health, density, projectile, fire-rate, movement, projectile-count, and boss-cadence multipliers. Abyss Mine also scales pressure gain, damage cadence, and the number of boss hazards. Plastic Vortex scales current force and debris rotation. Both bosses add denser patterns on higher difficulties.

Simulation entities continue to use prewarmed fixed-capacity pools. Mission JSON is parsed and expanded before the simulation begins; the update and rendering loops do not parse data. Completing sector 8 with at least one star unlocks sector 9. Completing sector 9 unlocks the existing sector 10 profile record without changing the profile schema.

All new visuals are original procedural geometry in `OceanRenderer`; no external assets were added.
