# Project Blue audio asset plan

## Current implementation

AssetManager loads each sound once and owns disposal. Music and SFX have separate saved enable flags and volume values, and master mute is saved independently. Pause and background transitions pause music and stop active one-shot sounds; resume restores music only when allowed by the saved settings. Missing audio files degrade to silence. The shared bitmap font also comes from AssetManager and is not reloaded by the menu skin.

The three current WAV files are original mathematical synthesis from `tools/GenerateAssets.java`. They are licensed and verified, but they are production placeholders:

| File | Channel | Current use |
|---|---|---|
| `assets/audio/ocean.wav` | Music | Eight-second looping ambience on menus and gameplay |
| `assets/audio/pulse.wav` | SFX | Primary shot and sonar pulse at different gains |
| `assets/audio/collect.wav` | SFX | Waste, salvage and rescue collection |

## Required final set

| Scene/system | Required cues | Direction |
|---|---|---|
| Boot and loading | soft sonar wake, load complete | Short, calm, no loud transient |
| Main menu and Level Select | menu ambience, move, confirm, back, locked | Spacious research-vessel interior blended with distant water |
| Hangar and upgrades | equipment select, purchase, insufficient salvage, maximum | Mechanical but civilian; avoid fighter-jet UI conventions |
| Dive ambience | one loop per ocean region or a layered adaptive bed | Underwater pressure, distant wildlife and restrained rhythm; seamless loops |
| Player weapons | pulse, spread, laser, torpedo, support drone | Distinct timing and pitch profiles with controlled repetition variance |
| Recovery actions | cleanup beam loop/start/stop, waste cleared, rescue progress, rescue complete | Positive and organic, separate from score pickup sounds |
| Defensive systems | shield absorb, shield break, sonar charge, sonar pulse | Clear midrange cues that remain audible on phone speakers |
| Player feedback | hull hit, critical hull, warning, dive failed | Short, non-startling and compatible with reduced-flash mode |
| Enemies | shot families, shield carrier, repair, destroy | Group by enemy function rather than one file per visual variant |
| Bosses | telegraph prepare, phase transition, exposed core, defeated | Warning cue must precede danger and have a consistent signature across all ten bosses |
| Environment | net bind, oil exposure, thermal rise, pressure rise, current reversal | Loops must fade cleanly when the state ends or the app pauses |
| Result and finale | star reveal, restoration swell, unlock, campaign finale | Satisfying but brief; result narration must remain readable over it |

## Technical delivery

- SFX: mono WAV or OGG, 44.1 kHz, normalized consistently, trimmed start latency.
- Music/ambience: streaming OGG, seamless loop points documented in samples and seconds.
- Provide dry masters and game-ready exports. Avoid baked-in limiter pumping.
- Cap rapid-fire concurrency by cue family and use small deterministic pitch/gain variation where repetition is obvious.
- New filenames use lowercase kebab case under `assets/audio/`.
- Add every file to the AssetManager queue, license inventory, SHA-256 inventory and Gradle allowlist together.
- Verify pause/resume, Bluetooth interruption and Android background behavior on a device before release.

No final sound may be added without written provenance and commercial-use terms. Generated or commissioned work must identify the creator and the rights granted to the project. Library sounds must retain the invoice/license text required by their terms.
