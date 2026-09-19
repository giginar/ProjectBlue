# Sectors 6 and 7

## Silent Reef

Silent Reef is sector 6. NEREID's Resonance Engine has flooded a formerly quiet reef with sonar pollution. The mission uses a purple, deep-blue, and bioluminescent cyan procedural palette.

The reusable `SonarSystem` owns energy, pulse cost, passive regeneration, pickup recharge, and reveal timing. Its authored values live in `config/silent-reef.json`. Touch players use the `PULSE` button above the lower-right play area; keyboard players can use Space or S. Movement remains relative drag and auto-fire remains unchanged.

Concealed enemies, reef obstacles, rescue creatures, and the Resonance Engine's moving weak points can only be targeted while a pulse is active. Concealed attacks still show a warning marker before firing. Empty energy only prevents another pulse; it does not add a separate punishment. Sonar cells and passive regeneration restore the resource.

The enemy set is EchoHunter, SoundMine, SilentStalker, and ResonanceDrone. The Resonance Engine progresses through sonar waves and mines, a false-target field, and two moving sonar-gated weak points before its core reopens.

## Frozen Depths

Frozen Depths is sector 7. The mission takes place below a NEREID drilling facility and uses ice blue, white, and thermal orange procedural colors.

The reusable `ThermalSystem` owns heat accumulation, passive and cold-zone recovery, the danger threshold, and progressive damage. Its authored values live in `config/frozen-depths.json`. Falling ice shows a warning ring before it drops. Thermal vents raise heat, blue cold zones remove it, and the HUD shows the current thermal load.

Drill points are disabled by staying within cleanup range. Moving ice-wall boundaries periodically narrow and reopen the route; higher difficulties narrow it further. The enemy set is IceDriller, CryoDrone, ThermalMine, and HeatVentGuard.

The Borealis Drill progresses through drill-arm volleys and falling ice, thermal vents with tighter safe routes, and destructible cooling units. Destroying both units exposes the main engine.

## Difficulty and progression

Difficulty still changes health, density, projectile speed, fire rate, boss cadence, movement, projectile count, and phase pressure. Silent Reef also raises pulse cost and lowers effective regeneration. Frozen Depths raises heat gain, shortens warnings and damage intervals, and narrows ice-wall routes.

Completing sector 6 with at least one star unlocks sector 7. Completing sector 7 unlocks the existing sector 8 record. Profile schema 5 adds the two sector achievements; older profiles keep all level records, unlocked equipment, and achievement progress during migration.
