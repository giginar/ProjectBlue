# Meta-progression and screens

## Architecture assessment and scope

The existing vertical slice already separated the fixed-step, pure Java simulation from
rendering, input, platform services, and persistence. `ScreenRouter` owned transitions at
frame boundaries and retained the active run while paused. `SaveService` owned a v1
checksummed profile; `GameConfig` held a single mission's constants. These boundaries were
retained rather than replacing the game loop or renderer.

The progression foundation uses an immutable `RunSpec`, per-sector profile records,
configurable difficulties and Scene2D menus beside Game/Pause. Equipment now extends
that foundation with a v3 profile and JSON content; see
[equipment systems](EQUIPMENT_SYSTEMS.md). Blue Coast now supplies the Level 1 duration,
timeline and encounter rules while keeping the existing fixed-step loop and profile IDs.
TIDE / Kaia / Pulse Cannon is the default, including Kaia's cleanup bonus. The previous
procedural simulation fixture remains covered by tests on the preserved later-level path.

## Navigation and progression

Main Menu offers Play, Hangar, Achievements, Settings, and Credits. Play opens the
scrollable Level Select chart. Select an unlocked card to see its briefing, difficulty
buttons and Begin Dive action. Hangar links to Submarine Select, Pilot Select, Weapon Select
and Upgrades.
Result offers replay of the same sector/difficulty, Level Select, Hangar and Main Menu.
Back/Esc follows the parent screen; within Level Select it returns from briefing to chart.

- There are ten stable sector IDs, 1 through 10. Only sector 1 / Normal starts open.
- A completion with at least one star opens the next sector. Sector 10 never creates an 11th.
- Each sector separately unlocks Normal, Hard, Expert, then Abyss. Failure unlocks nothing.
- Old sectors and completed difficulties remain playable. There are no energy or ad gates.
- Records independently retain highest stars, score, cleanup and rescue percentages.
  The highest completed difficulty is derived from the saved completion flags.
- Failed dives may improve score/ecology records and retain collected salvage, but earn no
  completion stars. Abandoning a dive through Pause does not record a result or earnings.
- UI disabled states, `ScreenRouter`, `GameScreen` construction, and `Profile.record` all
  enforce the same progression rules. Results are committed once when leaving a finished run.

## Authored data and combat

`core/src/main/resources/config/campaign.properties` is a Java resource packaged in both
desktop jars and Android APKs. It is configuration rather than a downloaded game asset.
Each sector keeps its stable name, region, seed and record ID. Blue Coast is the only playable
authored sector in this pass. Sectors 2-10 preserve save compatibility and appear as coming later.

| Difficulty | Health | Shot speed | Spawn density | Fire rate | Boss cadence | Fan shots | Movement | Phases |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Normal | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Hard | 1.25 | 1.2 | 1.25 | 1.15 | 1.2 | 3 | 1.2 | 2 |
| Expert | 1.55 | 1.45 | 1.6 | 1.35 | 1.5 | 5 | 1.5 | 3 |
| Abyss | 1.9 | 1.75 | 2 | 1.6 | 1.9 | 7 | 1.9 | 3 |

Density increases enemy count without changing Blue Coast's authored event order. Fixed pools
remain bounded and hostile shots have a mission limit. The Shoreline Compactor enters at 240
seconds. Its core gates two telegraphed press-arm phases, then two discharge pipes must be closed
before the final exposed-core phase. A living boss at the deadline fails the mission.

Combat uses enemies actually encountered. Cleanup uses authored waste and coral damage, Rescue
uses three turtles, and Integrity uses the loadout's maximum hull plus coral protection. Loadout
is captured at run creation. See [Blue Coast](BLUE_COAST.md) for the mission details.

Three vessels, four pilots and five weapons use permanent unlock conditions from JSON.
Six upgrades have up to five configured levels, increasing prices and atomic purchases.
Twelve local achievements track run conditions, campaign facts and cumulative progress;
unlocks queue persistent in-game notifications. Definitions, formulas, migration mappings
and provisional balance are documented in [equipment systems](EQUIPMENT_SYSTEMS.md).

## Persistence, migration and development builds

Profile v3 stores per-sector unlocks/records/completed difficulties, salvage, completed
runs, crew/vessel/weapon selection, earned equipment access, upgrade levels, achievement
progress/unlocks/pending notices, cumulative counters, audio settings and reduced motion.
No advertising preferences or SDK state are introduced.

The codec verifies CRC32 before normalization. v0 migrates settings and score only. v1
maps the original sector's score and proven completion to sector 1 / Normal, retaining
salvage and audio settings; historical cleanup/rescue percentages remain zero because
the old schema did not contain them. Higher difficulty completions are never invented.
v2 migration preserves all sector records, selections through explicit ID mappings, paid
Hull/Pulse levels and legacy Magnet reach; a frozen checksummed fixture verifies this.

Writes use a temporary file and a verified backup. Corrupt/truncated/oversized input tries
the backup, then starts a fresh profile if neither copy is usable. Menus explain recovery
and write failures; Settings includes Save Profile / Retry. Reset preserves the in-memory
profile identity used by audio, refreshes both disk copies, and requires a development flag.

- Desktop defaults to a production build. `-PdevelopmentBuild=true` generates the development
  flag and exposes Reset Profile in Settings; it requires a second in-game confirmation.
- Android uses `BuildConfig.DEBUG`; the control is present in debug and absent in release.
- No runtime key combination or launch argument enables reset in a production build.
- Desktop saves remain `%USERPROFILE%/.projectblue/profile.properties`; Android uses private
  application files. Smoke tests exclusively use `build/smoke/profile`.
- Exiting/backgrounding saves the profile. A new process opens Main Menu; unfinished dives
  are not serialized.

## UI and safe areas

The original procedural underwater backdrop, palette, font and audio remain in use.
Scene2D panels and star icons are generated locally. No external artwork was added.
Menus use an ExtendViewport with a minimum 540 x 720 logical area, a bounded content
width, scrollable content and a persistent Back control. Buttons are at least 84 logical
units high (about 50 dp at a 320 dp portrait width). Wide and short displays can scroll
without shrinking the entire menu to the gameplay rectangle. The gameplay FitViewport
remains 540 x 960.

Android's existing system-bar/display-cutout insets continue to pad the game View before
either viewport receives its size. Physical device touch/cutout validation is still needed;
no Android device or emulator was connected during this implementation.

## Verification

Commands executed on Windows:

```powershell
.\gradlew.bat :check
.\gradlew.bat :lwjgl3:run --args=--smoke
.\gradlew.bat :lwjgl3:run --args=--smoke-reload
.\gradlew.bat :android:assembleDebug :android:lintDebug :android:generateReleaseBuildConfig :lwjgl3:installDist
```

- 130 JUnit invocations pass, including the original regression checks.
- Real LWJGL/OpenGL smoke covers menus including Weapon Select, a full Blue Coast dive, drag,
  pause/resume, lifecycle, locked routes, automatic unlocks, disk persistence, replay,
  selections, exactly-once purchases/toggles, narrow/wide layouts, and boss rendering.
- The reload check launches a separate application process and verifies the saved records,
  level/difficulty locks, salvage, loadout, upgrades, achievement and audio preference.
- Android debug APK and desktop distribution build. Android lint reports 0 errors and
  5 warnings. The campaign resource is present in the APK. Generated Android release
  `BuildConfig.DEBUG` and default desktop `BuildInfo.DEVELOPMENT_BUILD` are false.
- The smoke driver guards against GLFW re-entering `render()` during a window resize;
  otherwise the automation could skip navigation checks. This was verified with debugger
  logpoints; no debugger changes or breakpoints are required to run the app.
- Asset license/hash verification and `git diff --check` pass.

Reports: `core/build/reports/tests/test/index.html`,
`android/build/reports/lint-results-debug.html`.
Screenshots: `build/smoke/01-menu.png` through the numbered scenario captures, including
`07-result.png`, `13-phone-settings.png`, `14-wide-credits.png`, `15-reloaded.png`,
`16-warden.png` and `17-weapons.png`. APK: `android/build/outputs/apk/debug/android-debug.apk`.

## Manual acceptance checklist

1. Run `.\gradlew.bat :lwjgl3:run -PdevelopmentBuild=true`. If using an existing test
   profile, choose Settings > Reset Profile > Confirm. This erases that local profile.
2. Open Play. Check that only sector 1 is open and its card shows name, region, empty stars,
   best score and cleared difficulties. Try sector 2: it must remain disabled.
3. Open sector 1. Only Normal is enabled. Start, drag to steer, collect plastic, rescue
   turtles, pause and resume. Disable the Shoreline Compactor and complete recovery with at least one star.
4. Result should show stars and records, plus sector 2 and sector 1 / Hard unlock messages.
   Return to Level Select. Sector 2 / Normal should be playable; its Hard should remain locked.
5. Replay sector 1 / Normal. A worse result must not lower any existing best record.
   A failed run must not unlock the next difficulty. Pause > End Dive must not add rewards.
6. Complete sector 1 / Hard, then Expert, defeating the Shoreline Compactor before the deadline.
   Check that each completion opens only the next difficulty of that sector. Abyss opens
   after Expert; check faster fire, denser drones and faster boss attacks.
7. After unlocking sector 2, select MANTA, Neri and Spread Cannon; buy one Hull upgrade
   with at least 30 salvage. Confirm a single deduction and upgrade level. A new dive
   should start with 95 hull, three-shot spread and faster rescues. Insufficient funds and maximum upgrades must disable buying.
8. Toggle sound/music, cycle volumes, enable reduced menu motion, and inspect Achievements.
   Exit the application completely and launch it again. Verify progress, records, settings,
   selections, upgrade levels and salvage are unchanged.
9. Visit every screen and use Back/Esc. Scroll to sector 10, the bottom of Settings and the
   last achievement. Resize desktop to 320 x 640 and 960 x 540; controls must stay reachable.
10. Run without `-PdevelopmentBuild=true`: Reset Profile must be absent. Install the debug
    APK on a phone: it should be present. Check touch/scrolling, cutout clearance, system
    back, background/resume, sound and a full dive. Repeat on a wide/tall screen if available.
11. Optional isolated corruption test: run both smoke commands, close the app, replace
    `build/smoke/profile/profile.properties` with invalid text, and run `--smoke-reload`.
    The verified backup should restore the profile. Production saves need not be edited.

## Changed files

Paths below are relative to their module's `src/main/java/com/projectblue/game/` unless noted.

| Area | Files |
|---|---|
| Composition/configuration | `core/.../ProjectBlueGame.java`, `config/GameConfig.java`, new `config/CampaignConfig.java`, `config/Difficulty.java`, `config/RunSpec.java`, `config/Loadout.java`, `core/src/main/resources/config/campaign.properties` |
| Simulation/presentation | `logic/GameWorld.java`, `logic/Entity.java`, `logic/LevelResult.java`, `render/OceanRenderer.java`, `ui/Hud.java`, new `ui/MenuTheme.java` |
| Persistence | `save/Profile.java`, `save/ProfileCodec.java`, `save/SaveService.java`, `save/SaveStore.java`, `save/GdxSaveStore.java`, new `save/LevelRecord.java`, `save/ProfileMigrations.java`, `save/Achievement.java` |
| Existing screens/router | `screens/ScreenRouter.java`, `screens/GameScreen.java`, `screens/MainMenuScreen.java`, `screens/ResultScreen.java` |
| New screens | `screens/StageMenuScreen.java`, `screens/LevelSelectScreen.java`, `screens/HangarScreen.java`, `screens/SubmarineSelectScreen.java`, `screens/PilotSelectScreen.java`, `screens/UpgradesScreen.java`, `screens/AchievementsScreen.java`, `screens/SettingsScreen.java`, `screens/CreditsScreen.java` |
| Platform/build | `platform/PlatformService.java`, `core/build.gradle`, `android/build.gradle`, `android/.../android/AndroidPlatformService.java`, `lwjgl3/.../lwjgl3/DesktopLauncher.java`, `lwjgl3/DesktopPlatformService.java`, `lwjgl3/DesktopSmokeGame.java` |
| Tests | Updated `core/src/test/java/com/projectblue/game/save/SaveServiceTest.java`; new `config/DifficultyTest.java`, `save/ProgressionTest.java`, `save/ProfilePersistenceTest.java` under the same test package root |
| Documentation | `README.md`, `docs/META_PROGRESSION.md`, `docs/LOCAL_PACKAGING.md` |
