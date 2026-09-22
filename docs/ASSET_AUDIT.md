# Asset audit

Audit date: 2026-09-19. Custom packaged asset gate: **PASS**.
Store asset readiness: **READY FOR PLAY CONSOLE REVIEW**; exports are under `store/assets/google-play/ocean-guard/`.

## Scope and method

Compared every physical file under `assets/` with the build allowlist, the exact keys in
[`verified-assets.properties`](../assets/licenses/verified-assets.properties) and the paths in
[`ASSET_LICENSES.md`](../assets/licenses/ASSET_LICENSES.md). Inspected procedural drawing,
font/audio generation, Android vector resources, Windows icon generation and store plans.
Regenerated the five custom media files in `build/final-qa/regenerated/`, leaving checked-in
assets untouched. All five outputs matched byte for byte. No new media was added.

## Packaged custom media

Paths below are relative to `assets/`. Each is original project-generated content with no
external sample or downloaded font input. They are usable within Project Blue under the
provenance recorded in the inventory; a general source-code distribution license has not
been selected by the owner.

| File | Bytes | SHA-256 | Source / release status |
|---|---:|---|---|
| `audio/pulse.wav` | 4012 | `e51667e0b843674f0ddfef7cd6ddefd277d08029c9bd5e6fd63cbe7eb2727184` | Mathematical sine sweep; placeholder firing sound |
| `audio/collect.wav` | 12392 | `7dbe1e579fc8b691090570b15e1fc786a053d18a5bc8e89bebba5be6bf785e11` | Synthesized harmonics; placeholder collection sound |
| `audio/ocean.wav` | 352844 | `ada2c97c1122537f3f61e434b07bddc61dd45437e6beb50e2beb95922e06de74` | Eight-second synthesized loop; placeholder ambient/music bed |
| `fonts/blue.fnt` | 8575 | `4867f8c8dc32db395a22786c6576168b3ffd70e511729559920bacf51fbdfeb2` | Original 5x7 glyph metrics; placeholder Blue Grid font |
| `fonts/blue.png` | 5289 | `75884630d2180fd684e945920b167a1c09bf27e0e9f51ce1f8b475f7b05e5593` | Original glyph atlas; placeholder Blue Grid font |

Generator: [`tools/GenerateAssets.java`](../tools/GenerateAssets.java).
The two additional files in `assets/licenses/` are inventory metadata, not game media.
No blanket license-directory exclusion remains. New files, stale hashes, extra/missing
hash keys and missing documented paths fail `verifyAssetLicenses`.

## Runtime, platform, dependency and store assets

| Group | Location / provenance | Audit result |
|---|---|---|
| Submarines, all ten environments, enemies, bosses, wildlife, divers, projectiles, pollution, warnings and recovery visuals | `core/src/main/java/com/projectblue/game/render/OceanRenderer.java` and mission data | Original procedural shapes and palettes; inventory expanded to cover all sectors; placeholders |
| Menu panels, stars, HUD, finale, loading treatment | `ui/`, `screens/` | Original generated geometry/pixmaps; no external skin; placeholders |
| Android launcher icon | `android/src/main/res/drawable/ic_blue.xml` | Original vector; inventoried placeholder; not an approved Play icon export |
| Windows icon | `tools/GenerateWindowsIcon.java` -> generated `ProjectBlue.ico` | Original project geometry; inventoried; generated packaging output, not custom APK media |
| Emergency fallback font | Embedded libGDX default font, used only if custom font loading fails | Dependency asset under libGDX Apache 2.0; recorded separately; not copied into `assets/` |
| Android/AdMob/UMP library resources | Resolved Gradle dependencies | Dependency-owned resources, not authored game/store art. Their own distribution terms remain applicable; preserve notices. The five-file game allowlist does not replace a dependency license review. |
| QA screenshots | Ignored `build/smoke/` | Actual desktop smoke output; not packaged and not approved for store use |
| Store icon / feature graphic / phone screenshots | `store/assets/google-play/ocean-guard/` | Original generated icon and feature graphic plus seven final 1080 x 1920 game captures; provenance recorded |

Source inspection found no evidence of copied Sky Force or other game content. The five
reproducible asset hashes establish their local generator provenance; this is not a universal
copyright or trademark clearance. No custom file with an unknown external license was admitted
to the release build. Future externally sourced content requires its actual commercial-use
terms and any attribution/purchase evidence before changing the allowlist.

## Explicit placeholder inventory

All five physical media files above; all runtime submarine/enemy/boss/wildlife/environment
art across sectors 1-10; HUD/menu/finale ornaments; the Android launcher vector and generated
Windows icon. There are no separate recorded voice lines, licensed music tracks, bitmap
character sheets or approved store graphics. Replacing placeholders is a production-art task,
not work completed by this audit.

## Reproduce

```powershell
.\gradlew.bat :verifyAssetLicenses
powershell -NoProfile -File tools/test-asset-licenses.ps1
```

The probe script creates one uniquely named temporary file, expects the gate to reject it,
then removes only that file in `finally`. It does not delete or rewrite existing assets.
Any later custom media replacement invalidates this audit until inventory, provenance and
hashes are reviewed again. See [store asset plan](STORE_ASSET_PLAN.md).
