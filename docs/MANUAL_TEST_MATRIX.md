# Manual release acceptance matrix

Date: 2026-09-19. Android execution was completed on a Huawei SNE-LX1 (Android 10/API 29)
and an x86_64 phone emulator (Android 16/API 36). ADB-driven checks are recorded as device
evidence, not as human touch/audio evidence. Remaining items explicitly say
`MANUAL VERIFICATION REQUIRED`.
Record tester, date, artifact SHA-256, OS/API, model, RAM, density, refresh rate, navigation
mode and result for every run. Attach screenshots/logs without personal data.

## Prompt 13 requested device scope

| # | Requested check | Result and evidence |
|---:|---|---|
| 1 | Clean install and first launch | PASS on both targets; physical debug data was uninstalled/reinstalled and fresh profiles were created |
| 2 | Update over existing install | PASS on both; `firstInstallTime` stayed stable while `lastUpdateTime` advanced |
| 3 | Main menu and level select | PASS on both with captured screens |
| 4 | Blue Coast gameplay | PASS on both, online and with all radios disabled |
| 5 | Completion and next-level unlock | **MANUAL VERIFICATION REQUIRED**; failed result was reached, successful clear/unlock was not |
| 6 | Save, close and reload | PASS on both; persisted Sound/Music changes survived force-stop and cold start |
| 7 | Touch movement and fire | PASS for ADB drag and visible auto-fire; physical feel remains manual |
| 8 | Second finger/multi-touch | **MANUAL VERIFICATION REQUIRED**; regression unit test passes, ADB did not provide simultaneous human fingers |
| 9 | HUD touch areas | PARTIAL PASS: pause edge tap worked on both; exhaustive physical measurement is manual |
| 10 | Portrait view | PASS on both |
| 11 | Different aspect ratios | PASS at 1080x2340, 1080x2400 and emulated 720x1280; tablet/foldable remains manual |
| 12 | Display cutout/safe area | PASS for real 90 px and emulated 132 px top cutouts; rotated/asymmetric cases are manual |
| 13 | Home background/foreground | PASS on both; return opened Pause |
| 14 | Back key and Android back gesture | PASS for `KEYCODE_BACK`; **MANUAL VERIFICATION REQUIRED** for edge gesture navigation |
| 15 | Screen off/on | PASS on both; return opened Pause |
| 16 | Close from recent apps | PASS on emulator by dismissing its card; **MANUAL VERIFICATION REQUIRED** on physical Huawei recents UI |
| 17 | Process death/reopen | PASS on both with force-stop/cold launch and valid persisted profile |
| 18 | Airplane-mode launch | PASS on both using `cmd connectivity airplane-mode enable`; mode was restored afterward |
| 19 | Gameplay without internet | PASS on both with Wi-Fi and mobile data disabled; radios were restored afterward |
| 20 | UMP consent flow | PARTIAL: runtime request passed and returned non-EEA (`gdprApplies=0`); **MANUAL VERIFICATION REQUIRED** for forced EEA form/choices |
| 21 | Rewarded ad flow | PASS on API 36 emulator with official Google Test Ad: show, `Reward granted`, dismiss, one-time continue |
| 22 | Interstitial ad flow | **MANUAL VERIFICATION REQUIRED**; three-win/later-session placement was not reached |
| 23 | Ad-load failure fallback | PASS on both offline: both test ad loads returned error 0 and gameplay/navigation continued |
| 24 | R8/minified QA APK launch | PASS on both using debug-signed `com.projectblue.game.qa`; an API 29 early-resume crash was fixed and the rebuilt APK passed locked-screen/repeated cold starts; ads disabled; not a production artifact |
| 25 | Boss and result screen | PARTIAL: failed Blue Coast result PASS on emulator; **MANUAL VERIFICATION REQUIRED** for on-device boss encounter/clear |
| 26 | Audio pause/resume | Lifecycle/audio state path passed; **MANUAL VERIFICATION REQUIRED** for audible confirmation |
| 27 | Texture/context recovery | Process/surface recreation and desktop managed-texture smoke passed; **MANUAL VERIFICATION REQUIRED** for forced Android EGL context loss |
| 28 | Crash/ANR/important logcat review | PASS after fix: one API 29 minified-QA early-resume crash was captured, retraced, fixed and rerun with an empty crash buffer; no ANR, StrictMode or GL invalid-operation record. Huawei logs show a recoverable EGL 0x3004 line followed by successful Mali-G51 GLES 3.2 initialization |

## Device coverage

| Environment | Required coverage | Status |
|---|---|---|
| API 26 phone, low-memory class | Minimum API startup, all mission configs, save/migration, audio, 30-minute soak | Pending |
| API 29 Huawei SNE-LX1 with cutout | Debug/minified QA startup, menu/gameplay, save, lifecycle, offline/airplane and logs | Partial pass; manual multi-touch/audio/boss/soak remain |
| API 33-35 tall phone with cutout | Two-finger control, consent, gesture navigation, background/context loss | Pending; API 29 and 36 do not replace this band |
| API 36 phone emulator | Debug and local minified QA APK, Back key, offline/airplane cold start, rewarded test ad | Partial pass; AAB-generated/signed APK, predictive gesture and forced UMP remain |
| API 36 tablet/foldable or resizable window | Portrait request handling, large-screen orientation policy, safe bounds and resize | Pending |
| 16 KB page-size Android environment | Native library loading and actual gameplay from AAB-generated APK | Pending |
| Narrow phone around 360 dp wide | HUD legibility, story wrapping, touch targets and large-UI settings | Pending |

Use the final signed/minified artifact for release sign-off. Debug official demo ads may be
used to exercise advertising safely; repeat relevant lifecycle tests on the chosen release
configuration. See [release steps](RELEASE_STEPS.md).

## Gameplay and mobile cases

| ID | Steps and expected result | Existing evidence / device status |
|---|---|---|
| G-01 | Drag from several initial touch points; submarine moves relatively, stays in bounds, stops on release/cancel. Touch outside letterboxing must not move it. | ADB drag moved the submarine correctly on both devices; boundary/feel **MANUAL VERIFICATION REQUIRED** |
| G-02 | Hold movement with one finger; use the second on sonar and pause; resume and continue dragging without pointer theft or stuck input. | Second-pointer regression passes; simultaneous physical input **MANUAL VERIFICATION REQUIRED** |
| G-03 | Fire automatically while stationary/moving. Compare all five weapons against drones, protected coral, nets, boss parts and shielded targets. No shots while paused. | Auto-fire visible on both devices; all-weapon/target matrix **MANUAL VERIFICATION REQUIRED** |
| G-04 | Take projectile/contact/hazard damage, exhaust and regenerate shield, reach zero hull. Invulnerability must prevent repeated immediate hits; failed run earns zero stars. | Pure gameplay tests; Pending |
| G-05 | Compare pilot/vessel passive bonuses. Activate sonar where available, exhaust energy and wait for recharge; verify hidden targets and gated weak points. | Loadout/sonar tests; no universal active special ability; Pending |
| G-06 | Approach waste, hold the cleanup beam, interrupt range and restart. Check oil valves, debris clusters, combo and result percentages. | Cleanup/environment tests; Pending |
| G-07 | Rescue each authored species/diver; interrupt rescue, let a timed signal expire, free multiple creatures and observe despawn. Weapons must not kill rescue creatures. | Rescue/net tests and freed-vortex pool regression; Pending |
| G-08 | Damage coral with every weapon and enemy hazard; verify habitat health and Cleanup/Integrity decrease without granting kills or salvage for coral. | Rules/laser regression; Pending |
| G-09 | Pause with button, keyboard/back; lock screen, Home, task switch and resume. Simulation/audio stop; returning requires Resume Dive and clears stale touch. | Android pause/Home/screen-off/Back lifecycle passed on both; audible audio check manual |
| G-10 | Finish/fail/replay a mission. Verify category totals, 0-3 stars, salvage, before/after view and navigation. No duplicate settlement on repeated taps. | Failed Blue Coast result passed on emulator; successful finish/replay **MANUAL VERIFICATION REQUIRED** |
| G-11 | Complete every boss phase through normal controls, observe warnings and invulnerable gates. Reach recovery and wait for result transition. | Boss state-machine tests; full human route Pending |
| G-12 | Finish NEREID Core, escape, inspect finale, return to menus and replay. Let escape time out, earn the single continue and finish; second continue must be unavailable. | Four-difficulty completion/continue regression with assisted combat; Pending |
| M-01 | Check 16:9, tall portrait, narrow phone, tablet and multi-window. No stretching or hidden controls; measure cutout/system-bar clearance on both sides. | Phone ratios/cutouts passed; tablet, multi-window and both-side rotation **MANUAL VERIFICATION REQUIRED** |
| M-02 | Back from every menu, gameplay, pause, result, finale, privacy form and ad. No accidental duplicate screens, stuck pause or reward. | Gameplay/Pause Back and rewarded dismissal passed; all-screen gesture route manual |
| M-03 | Background under memory pressure, kill the background process, cold start. Completed progression survives; interrupted dive returns to menu rather than a fabricated restored run. | Force-stop/cold-start and setting reload passed on both; OS low-memory eviction manual |
| M-04 | Cold start in airplane mode with fresh app data, then with an existing profile. Menus/gameplay must work without waiting for consent or ads. | Airplane launch and no-network gameplay passed on both targets |
| M-05 | Measure all touch hit rectangles, HUD font and contrast at physical scale; test large UI, reduced motion, reduced particles/background and sound/music controls. | Pause edge target and saved Sound/Music passed; full accessibility measurement manual |
| M-06 | Play/retry/visit every screen for 30 minutes, rotate/resize where OS permits, repeatedly background and resume. Track frame time, Java/native heap, audio and managed textures. | Fixed pool/source ownership review and desktop texture reload smoke; device soak Pending |

## Progression and persistence cases

| ID | Steps and expected result | Existing evidence / device status |
|---|---|---|
| P-01 | Fresh profile: only sector 1 Normal opens. Clear each of ten sectors; check sequential sector unlocks. | Progression/config tests; Pending |
| P-02 | For each sector clear Normal, Hard, Expert, Abyss in order. Other sectors/difficulties must retain independent records and locks. | Progression/difficulty tests; Pending |
| P-03 | Replay with better/worse score, stars, Cleanup and Rescue independently; each best record only improves. Failure must not count as a clear. | Rules/progression/persistence tests; Pending |
| P-04 | Earn/spend salvage; buy every upgrade at exact cost, with insufficient balance and at maximum level. Repeated taps/save failure cannot spend twice. | Economy/loadout/purchase rollback tests; Pending |
| P-05 | Select all unlocked pilots, vessels and weapons, restart, confirm selections and actual stats. Locked choices stay locked. | Equipment tests/GL smoke; Pending |
| P-06 | Trigger all 16 local achievements, check one-time rewards/notifications and persistence. Final completion unlocks the expected final achievement. | Achievement/finale tests; Pending |
| P-07 | Upgrade from older schema profiles, including the frozen v2 fixture and v3/v4 data. Keep equipment, salvage, records and settings without inventing completion. | Schema 0-5 compatibility tests; Android upgrade Pending |
| P-08 | Corrupt primary save, retain valid backup; then corrupt both. Confirm backup recovery or visible safe-default status. Simulate failed write/low storage and retry. | Checksums, backup, atomic/temp writes and failure tests; device Pending |
| P-09 | Exit after final completion, launch again, inspect final recovery record and replay all sectors. Never re-grant first-clear rewards. | Finale/save tests and smoke reload; Pending |

## Advertising cases

| ID | Steps and expected result | Existing evidence / device status |
|---|---|---|
| A-01 | Inspect debug App ID/units: official Google demos only. Inspect chosen release flags; ads disabled unless explicitly provisioned. | Build task passed; runtime displayed Google's `Test Ad`; QA/release-style ads disabled |
| A-02 | Rewarded: earn+dismiss, close early, show failure, double callback, stale screen, save failure and process death. Only a valid earned+dismissed event may settle once. | Real earned+dismissed test ad passed on API 36; negative cases remain covered by `AdsIntegrationTest` |
| A-03 | Fail, accept continue, resume, fail again; one continue per run. Verify +60 seconds, hull/protection and incremental salvage without paying the first result again. | Real test-ad continue reached Pause and unit policy passes; second post-continue failure **MANUAL VERIFICATION REQUIRED** |
| A-04 | First session has no interstitial. Later require three successful runs and both three-minute timers; no ads during play, failure or replay, or after a reward on the result. Test clock rollback. | Interstitial policy tests pass; real placement **MANUAL VERIFICATION REQUIRED** |
| A-05 | Exercise required/not-required UMP regions, denial, consent error, cached permission and Privacy Options withdrawal. No requests before `canRequestAds`; old ads invalidated on privacy change. | Non-EEA runtime passed on both; forced EEA/denial/privacy choices **MANUAL VERIFICATION REQUIRED** |
| A-06 | Airplane mode, no-fill and repeated load/show failures must preserve result navigation and gameplay. Background during ad/form, then return. | Offline load failures and gameplay passed on both; background during form/ad manual |

## Per-sector content verification

The authored JSON, shared simulation, renderer and boss tests were reviewed for every row.
All sectors use the same data-driven GameScreen. Each has a distinct theme/problem, enemy
roster, rescue entries, boss state machine, recovery palette/result view and salvage rewards.
Midpoint entries below are scripted windows/messages; their presence alone does not prove a
distinct combat encounter. Blue Coast's fallback is an explicit gap (HP-04).

| Sector | Theme / problem / distinctive mechanic | Enemy roster | Rescue roster |
|---|---|---|---|
| 1 Blue Coast | Coastal waste; cleanup beam and protected coral | Scout, Sweeper, Net Launcher, Carrier, Repair, Turret | Sea turtles |
| 2 Coral Gardens | Extraction-damaged reef; cutters and shield protection | Coral Cutter, Shield Carrier, Burrow Drone | Seahorse, manta, reef fish |
| 3 Ghost Nets | Abandoned fishing grounds; drifting nets and timed rescue | Net Launcher, Net Recycler, Fast Hunter Drone | Turtle, seal, fish school |
| 4 Sunken City | Illegal waste in submerged ruins; moving corridors, chemical barrels and collapse warnings | Chemical Bomber, Ruin Turret, Salvage Mech, Ambush Drone | Research and rescue divers |
| 5 Black Tide | Oil pipeline pollution; cleanup valves and concealed hazards | Oil Spreader, Ignition Drone, Pressure Tanker, Pipeline Guard | Divers |
| 6 Silent Reef | Sonar pollution; energy-limited reveals and hidden weak points | Echo Hunter, Sound Mine, Silent Stalker, Resonance Drone | Reef fish, manta |
| 7 Frozen Depths | Sub-ice thermal waste; heat/cold and drilling warnings | Ice Driller, Cryo Drone, Thermal Mine, Heat Vent Guard | Seal, fish school |
| 8 Abyss Mine | Deep mining pressure; safe zones, power stations and sonar | Deep Miner, Pressure Drone, Rail Turret, Abyss Guardian | Research diver, fish school |
| 9 Plastic Vortex | Debris gyre; current reversal, clusters and cleanup combo | Vortex Drone, Trash Swarm, Magnetic Collector, Current Disruptor | Turtle, manta |
| 10 NEREID Core | Headquarters; combined restoration systems and timed final escape | Defense Drone, Net Interceptor, Pipe Guard, Recovery Hunter, Core Sentinel | Turtle, manta, seal, fish school definitions; additional final rescue signals |

| Sector | Midpoint start / window (seconds) | Multi-stage boss; configured start / deadline (seconds) | Boss salvage | Story / NEREID link |
|---|---|---|---:|---|
| 1 | 135 / 8, generic fallback | Shoreline Compactor; 240 / 360 | 50 | Coastal NEREID briefing; no explicit intro/midpoint text in JSON |
| 2 | 138 / 12, mineral convoy | Reef Breaker; 285 / 390 | 55 | Extraction reef; explicit mission messages |
| 3 | 142 / 12, current shift | Ghost Net Harvester; 295 / 400 | 65 | Fishing grounds; NEREID link indirect (MP-04) |
| 4 | 145 / 14, unstable structures | Urban Salvager; 285 / 395 | 80 | NEREID waste district; explicit mission messages |
| 5 | 152 / 14, pressure surge | Oil Kraken; 305 / 420 | 95 | NEREID pipeline; explicit mission messages |
| 6 | 150 / 14, false echoes | Resonance Engine; 305 / 420 | 105 | NEREID exclusion zone; explicit mission messages |
| 7 | 154 / 14, thermal surge | Borealis Drill; 310 / 425 | 115 | NEREID drilling facility; explicit mission messages |
| 8 | 156 / 14, drilling arms | The Harvester; 318 / 435 | 125 | NEREID mining; explicit mission messages |
| 9 | 154 / 14, current reversal | Recycler Leviathan; 315 / 438 | 135 | Gyre and NEREID collection operation; explicit messages |
| 10 | 168 / 14, combined recovery | Leviathan Core; 325 / 520 | 180 | NEREID headquarters, archives and final recovery; explicit messages |

For **each** sector and difficulty, record all of: intro/midpoint/story text, enemy variety,
rescue interactions, each boss phase and telegraph, Normal/Hard/Expert/Abyss differences,
before/after habitat change, score/stars/cleanup/rescue/salvage, next unlock and replay.
Configured boss start may be delayed by environmental gates, especially sector 10.
Salvage above is the base boss reward, not the total run payout or an advertised guaranteed
income. Difficulty affects spawn density, health, shots, cadence and subsystem pressure.

| Sector | Normal human clear | Hard human clear | Expert human clear | Abyss human clear | Automated full timeline |
|---|---|---|---|---|---|
| Blue Coast | Pending | Pending | Pending | Pending | Pass, all 4 |
| Coral Gardens | Pending | Pending | Pending | Pending | Pass, all 4 |
| Ghost Nets | Pending | Pending | Pending | Pending | Pass, all 4 |
| Sunken City | Pending | Pending | Pending | Pending | Pass, all 4 |
| Black Tide | Pending | Pending | Pending | Pending | Pass, all 4 |
| Silent Reef | Pending | Pending | Pending | Pending | Pass, all 4 |
| Frozen Depths | Pending | Pending | Pending | Pending | Pass, all 4 |
| Abyss Mine | Pending | Pending | Pending | Pending | Pass, all 4 |
| Plastic Vortex | Pending | Pending | Pending | Pending | Pass, all 4 |
| NEREID Core | Pending | Pending | Pending | Pending | Pass, all 4; assisted completion/continue also passes |

The timeline test restores hull each step and deliberately does not bypass boss gates:
every route terminates with zero-star failure by its deadline, without non-finite player
coordinates, exceeded hostile-bullet caps or dropped authored enemy counts. This is useful
integration evidence, but cannot certify that an ordinary player can clear a sector.
