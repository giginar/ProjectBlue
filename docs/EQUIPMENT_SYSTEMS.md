# Equipment, weapons and local achievements

## Content and lifecycle

All five content groups live in
[`core/src/main/resources/config/content.json`](../core/src/main/resources/config/content.json).
This UTF-8 Java resource ships inside the desktop core jar and Android APK. It contains
three submarines, four pilots, six upgrades, five weapons and sixteen achievements.
`ContentCatalog` parses it once at startup into immutable records. `Loadout.from(profile)`
composes the selected equipment and upgrades once for each dive. Gameplay never reads JSON.

Stable uppercase IDs are part of the save schema. Display names, descriptions, unlock
conditions, stats, upgrade costs/effects and weapon parameters are editable in JSON.
Adding a new stable ID also requires extending its enum registry; adding a new firing
mechanic requires a `WeaponBehavior` implementation. Renaming an ID needs a migration.

Validation rejects missing definitions/fields, duplicate IDs and object keys, unknown
behaviors/stats/metrics, non-finite or out-of-range numbers, fractional integer fields,
invalid unlock sectors, zero or more than five upgrade levels, gaps in level numbers,
negative or non-increasing prices, non-increasing cumulative effects and invalid pilot
passives. Each pilot must have one or two different passive stats, each at most 25%.
Config size is limited to 128 KiB. Errors identify the file and offending definition
where available and log a readable message to stderr (also visible in Android logs).

An invalid catalog is rejected as a whole. `config/SafeContent.java` contains a frozen,
independent emergency catalog with all required IDs, so missing/corrupt resources cannot
leave the starting equipment unavailable. Normal balance edits do not require changing
this emergency snapshot. Update it deliberately when adding registry IDs and verify the
fallback tests. No hot reload or downloaded configuration is included.

## Provisional balance

**Every number below is a starting point for playtesting, not final balance.**
Speeds use logical units/second, fire rates use shots/second; cleanup/rescue power are
multipliers against the original 0.42-second cleanup and 1.5-second rescue durations.

| Vessel | Hull | Speed | Damage | Fire rate | Cleanup | Rescue | Shield | Unlock |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| TIDE | 100 | 1100 | 10 | 5.556 | 1 | 1 | 0 | Initially |
| MANTA | 85 | 1320 | 9 | 5.556 | 1.2 | 1 | 0 | Sector 2 |
| LEVIATHAN | 140 | 825 | 13 | 3.85 | 1 | 1 | 20 | Sector 4 |

`specialAbility` describes a passive hull trait: Steady Current, Reef Sweep and Deep Guard.
Their effects are already represented in the numeric fields; they are not a second bonus
or an active ability button. Kaia starts available with +15% cleanup power. Atlas unlocks
at sector 2 with +15% maximum hull; Neri at sector 2 with +20% rescue speed; Rook after
25 enemy kills with +10% primary damage. Each currently has exactly one passive.

Upgrade effects are **cumulative totals at the given level**, added to vessel stats,
then multiplied by pilot bonuses. Hull/damage are rounded to integers. Each of the five
prices follows an authored increasing curve.

| Upgrade | First price | Effect at levels 1 / 5 |
|---|---:|---|
| Primary Weapon | 60 | +2 / +10 primary damage |
| Hull | 45 | +10 / +50 maximum hull |
| Cleanup Beam | 35 | +0.15 / +0.75 cleanup power |
| Shield | 50 | +10 / +50 starting shield |
| Rescue System | 35 | +0.1 / +0.5 rescue speed |
| Support Drone | 70 | Auxiliary shot damage 2 / 10 |

Shield absorbs hits before hull, refills only at the start of a dive, and does not
regenerate. Shield damage counts as damage for Untouched. Support Drone upgrades add
an auxiliary drone to any selected primary, including the Support Drone primary.

Tune effective damage per second, coverage and hit rate together. MANTA sacrifices hull
and per-shot damage for mobility/cleanup. LEVIATHAN has slower movement and cadence.
Pilot bonuses intentionally stay small. The full cost curve and ten-sector income simulation are
recorded in [`BALANCING.md`](BALANCING.md). At maximum upgrade levels, measure clear time,
survival, salvage earnings and whether one pilot or weapon dominates across all difficulties.
The existing profile IDs, difficulty locks, and star rules remain compatible. Blue Coast now
owns Level 1 duration, rewards, and encounter denominators. The default is TIDE / Kaia / Pulse
Cannon, so cleanup includes Kaia's bonus. `Loadout.standard()` remains the regression-test fixture.

## Weapons and selection

Hangar links to Submarine Select, Pilot Select, Weapon Select and Upgrades. Locked cards
show their unlock requirements. Earned access and all three selections persist.
Newly earned access is permanent even if a later config raises its threshold.

| Weapon | Unlock | Behavior | Damage / rate multipliers |
|---|---|---|---|
| Pulse Cannon | Initially | One forward pulse | 1 / 1 |
| Spread Cannon | Sector 2 | Three angled projectiles | 0.48 each / 0.75 |
| Focus Laser | Sector 3 | Immediate narrow beam, nearest target in its lane | 0.7 / 1.45 |
| Homing Micro-Torpedo | Sector 4 | Guided shot; reacquires the nearest live target | 1.8 / 0.55 |
| Support Drone | Sector 5 | Orbiting emitter aims at nearest target | 1.15 / 0.85 |

`WeaponController` selects a stateless `WeaponBehavior` from a registry, with independent
primary and auxiliary timers. Projectiles carry their damage and guidance values in the
existing fixed pool. The laser uses a reusable visual body; the drone/shield use original
procedural geometry. No external art, audio or dependencies were added.

## Transactions and persistence

Current profile schema **v5** preserves the equipment format introduced in v3, the existing
per-sector records, and independent difficulty flags. v0/v1 migrations remain supported; a
frozen checksummed v2 fixture exercises the equipment migration. Old selections map as follows:

- Minnow -> TIDE, Needle -> MANTA, Bastion -> LEVIATHAN.
- Marin and Sol -> Kaia, Neri -> Neri. Old freely available vessel/pilot access is retained.
- Hull -> Hull, Pulse -> Primary Weapon. Paid Magnet levels retain their +12 salvage reach
  per level through `legacyMagnetLevel`; there is no new Magnet purchase button.
- First Dive, Cleaner and Abyss achievements retain their equivalent unlock/progress.
  Legacy rescuer/explorer counters are archived in the profile. Historical kill totals
  were never recorded, and plastic progress was capped at 100; migration keeps that
  known lower bound rather than inventing missing history. It never infers Untouched.
- Existing sector unlock/clear and maximum-upgrade facts can award the corresponding new
  achievements on migration. Previously unlocked equivalents do not notify again.

`SaveService.purchase` is the only production purchase entry point. It serializes
transactions, checks funds/caps, mutates a candidate profile, writes that candidate,
and only then updates the existing live profile instance. Selection uses the same boundary.
A failed write changes neither equipment/upgrade level nor salvage and leaves a visible
retry message. The next dive uses a new snapshot; an active dive never changes.

`GdxSaveStore` writes/validates a temporary file, forces it to storage, preserves a verified
backup through its own temporary file, and atomically replaces the primary where supported.
Filesystems without atomic rename use replace-in-place only after the verified backup is durable.
Existing corruption recovery and Settings > Save Profile / Retry remain available.

## Local achievements

`Profile` retains absolute progress, permanent unlock flags and pending notifications.
`LocalAchievementService` delivers pending notices through the shared `AchievementToast`
overlay, one at a time across screen transitions. Notice acknowledgment is saved before
display, so reopening cannot repeatedly announce the same unlock. A process exit between
acknowledgment and rendering can lose that visual notice, but never the achievement.

| Achievement | Criterion |
|---|---|
| First Dive | Complete sector 1 |
| Clean Start | At least 80% cleanup in one finished dive |
| Perfect Blue | 100% cleanup in one finished dive |
| No One Left Behind | Rescue every creature in one finished dive |
| Untouched | Complete with zero actual hull or shield damage |
| Recycler I / II | 100 / 1,000 cumulative plastic |
| Drone Hunter | 100 cumulative enemies, including bosses |
| Deep Explorer | Unlock sector 5 |
| Guardian of the Blue | Complete sector 10 |
| Nightmare Below | Complete any sector on Abyss |
| Fully Equipped | Reach an upgrade's configured maximum level |
| Return the Reef Song | Complete sector 6 |
| Cool the Borealis | Complete sector 7 |

Run statistics commit once on the existing result transition. Failed dives count collected
plastic, destroyed enemies, cleanup and rescue; completion achievements require success.
An abandoned dive remains unrecorded, consistent with existing rewards/progression.
Counters saturate instead of overflowing. Repeat qualifying runs never re-unlock or
re-notify an achievement.

`AchievementService` exposes unlock and absolute progress operations.
`LocalAchievementService.synchronize(adapter)` can replay a complete snapshot to a future
idempotent provider adapter with local-to-provider ID mapping. No Google Play Games SDK,
sign-in, network permission or automatic remote synchronization has been added.

## Verification and manual scenarios

Verified on Windows: **160 JUnit invocations**, root asset license/hash checks, desktop
distribution, real OpenGL smoke and a separate application reload, Android debug APK,
and Android lint (0 errors, 5 existing warnings). The new tests cover malformed configs,
fallbacks, config effect changes, concurrent purchases, storage failure rollback, all
achievement criteria, notification persistence, all five firing strategies, shield damage,
live cleanup/rescue upgrade effects and v2 migration.

No Android device/emulator is connected. APK creation is verified; on-device touch,
lifecycle and rendering still need the manual checks below.

```powershell
.\gradlew.bat :check :lwjgl3:installDist :android:assembleDebug :android:lintDebug
.\gradlew.bat :lwjgl3:run --args=--smoke
.\gradlew.bat :lwjgl3:run --args=--smoke-reload
```

1. Start a fresh test profile. Confirm only sector 1 / Normal, TIDE, Kaia and Pulse Cannon
   are available. Other equipment cards show a requirement and a disabled button.
2. Complete sector 1. Confirm sector 2 / Normal and sector 1 / Hard open as before.
   Select MANTA, Neri and Spread Cannon; fully exit/relaunch and verify all three choices.
   Start a dive: hull is 85, spread has three lanes, rescue takes about 1.25 seconds.
3. Buy Hull with exactly 30 salvage: balance becomes zero, level becomes one and a new
   MANTA dive starts with 95 hull. Try another purchase with insufficient funds.
   Raise one upgrade to level five: buying is disabled and Fully Equipped appears once.
4. Repeat for Cleanup Beam, Shield, Rescue System and Support Drone. Verify faster cleanup,
   a visible shield that absorbs damage, faster rescues, and auxiliary drone fire.
   Check Primary Weapon damage with the combat tests or a controlled target.
5. Unlock sectors 3-5. Select Laser, Torpedo and Drone in turn. Check lane targeting,
   guidance/retargeting, orbiting aim and persistence. Switch equipment only between dives.
6. Finish dives at 28/36, 29/36 and 36/36 plastic (below 80%, above 80%, 100%), and rescue
   4/5 then 5/5 creatures. Check Clean Start, Perfect Blue and No One Left Behind.
   Complete once without hits and once with a shield hit: only the former grants Untouched.
7. Cross 100 and 1,000 total plastic and 100 enemy kills over multiple dives. Check capped
   progress, queued notices, reload persistence, and no repeated notices on replay.
   Unlock sector 5, complete sector 10 and clear Abyss for their achievements.
8. Load the checked-in v2 fixture in an isolated save directory. Check sector records,
   difficulty locks, salvage, renamed selections, paid upgrade levels and magnet reach.
   Do not overwrite a personal profile with a test fixture.
9. In an isolated test directory, deny profile writes and attempt a purchase. Confirm no
   deduction/level change, a visible save error, and successful purchase after storage
   becomes writable. Unit tests inject the same failure deterministically.
10. Temporarily break a copied config (duplicate ID, negative price or level 6), rebuild
    and launch. Check a readable log and safe starting content, then restore the config.
11. Install the APK on Android. Repeat selection/relaunch, purchases, a full dive,
    background/resume, scrolling, cutout clearance and Back navigation. Inspect all weapon
    effects and achievement notifications on a phone and a tablet.

Reports: `core/build/reports/tests/test/index.html`,
`android/build/reports/lint-results-debug.html`.
Desktop: `lwjgl3/build/install/lwjgl3/bin/lwjgl3.bat`.
APK: `android/build/outputs/apk/debug/android-debug.apk`.
Smoke screenshots include `build/smoke/17-weapons.png` and the existing numbered captures.
