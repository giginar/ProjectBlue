# Blue Coast

Blue Coast is the only authored playable sector in this content pass. It runs in the shared
`GameScreen` and `GameWorld`; later sectors can supply another validated mission config without
copying the screen or simulation.

## Data and runtime structure

- `core/src/main/resources/config/blue-coast.json` defines the timeline, environmental props,
  six enemy compositions, mission limits, and Shoreline Compactor tuning.
- `MissionConfig` validates the complete file once at startup. Duplicate IDs, missing references,
  invalid ranges, unsafe formation positions, and unordered events fail validation. A compact safe
  route is used with a clear error log if the packaged JSON is missing or malformed.
- `SpawnTimeline` expands density-adjusted waves once. Runtime updates only move a cursor; they do
  not read JSON or create timeline objects per frame.
- `EnemySystems` composes movement and weapon patterns with data-owned stats and rewards. Scout,
  Sweeper, Net Launcher, Armored Carrier, Repair Drone, and Turret Platform all use this path.
- Entity and projectile pools remain fixed. Blue Coast additionally caps hostile projectiles at 96.
- `ShorelineCompactor` is a pure state machine. Core health gates the press-arm phase, both pipes
  must close before the final core phase, and warning states drive the visual telegraphs.

## Provisional balance

These values are starting points for playtesting and are expected to change.

| Point | Current value |
|---|---:|
| Target duration | 300 seconds |
| Failure deadline | 360 seconds |
| Boss arrival | 240 seconds |
| Post-boss recovery | 6 seconds |
| Cleanup movement multiplier | 0.72 |
| Net movement multiplier / duration | 0.55 / 2.1 seconds |
| Boss core / each pipe | 1,350 / 100 health |
| Scheduled enemies / boss drone budget | 36 / 6 |
| Cleanup targets / turtles / coral areas | 52 / 3 / 4 |

Difficulty multipliers still come from `campaign.properties`. Enemy health, density, projectile
speed, firing rate, and boss cadence scale without changing the authored event order.

## Completion and persistence

Defeating the boss stops the timeline, removes hostile projectiles and active drones, and starts
the recovery sequence. The result records Combat, Cleanup, Rescue, Integrity, salvage, stars, and
the before/after restoration amount. A successful one-star result opens Sector 2 in the existing
profile format and updates local achievements. Sector 2 is shown as coming later and cannot launch.

## Manual checks

1. Start Blue Coast on Normal and confirm all six drone silhouettes and attacks appear over the
   authored waves. Confirm Net Launcher hits slow movement and Repair Drone heals a damaged ally.
2. Approach each waste type. Confirm the cleanup beam slows the submarine, larger targets take
   longer, and salvage increases once per completed target.
3. Rescue all three turtles. Fire into a coral area and confirm its color/health changes and the
   final Cleanup and Integrity scores are reduced. Turtles must never accept weapon damage.
4. At the boss, verify warning colors precede the press arms and pipe phase. Confirm core shots are
   ignored while pipes are active, then destroy both pipes and the exposed core.
5. Confirm no enemy or hostile projectile remains after defeat. Watch the water clear, coral color
   return, and fish population grow before the result screen shows the before/after panels.
6. Pause during an early wave and a boss warning. Resume after several seconds and confirm the
   timeline and warning continue from the paused time without a jump.
7. Complete with one or more stars, restart the app, and confirm records, salvage, achievements,
   and the Sector 2 unlock persist while Sector 2 remains marked `COMING LATER`.
8. Repeat on a tall phone aspect ratio and a wide desktop window. Confirm controls remain inside
   the gameplay viewport and boss telegraphs remain readable.

## Remaining art and tuning work

All new visuals are original runtime geometry. They are suitable placeholders for gameplay and
technical validation, but enemy silhouettes, impact feedback, environmental depth, boss destruction,
and the before/after presentation need an art polish pass. Music and effects still use the existing
original generated audio. Enemy health, salvage income, wave pressure, cleanup times, score thresholds,
and boss health need device playtests before release balancing.
