# Campaign balancing

This document records the assumptions used for the ten-sector campaign balance. All figures describe
the first clear of each sector on Normal. Hard, Expert, Abyss, and replays are additional income and
are deliberately excluded from the first-campaign budget.

## Progression goals

- A new profile starts with Sector 1, TIDE, Kaia, and Pulse Cannon.
- One successful sector should pay for several useful upgrade choices without requiring an ad or a
  replay. The model's cautious Sector 1 clear buys four of the cheapest upgrade ranks.
- A cautious first campaign should buy roughly 21 of 30 ranks. The expected model buys 23.
- Even a perfect first campaign cannot buy every rank. Replays and higher difficulties retain an
  economy purpose after the finale.
- A failed dive keeps collected salvage, ecology records, and achievement counters, but it never
  opens a sector or difficulty.

Upgrade prices live in `config/content.json`. Enemy, waste, boss salvage, and the per-run
`salvageCap` live in each sector's mission JSON. Economy changes therefore do not require Java
changes.

## Ten-sector salvage simulation

Each model assumes the boss is defeated and its configured reward is received. Enemy rate is the
share of scheduled Normal enemies destroyed whose salvage is collected. Waste rate is the share of
authored waste cleaned. The model does not count ads, achievements, or a completion bonus because
none of those award salvage. Boss-spawned enemies and optional mechanic rewards provide catch-up
income when scheduled drops are missed, but the configured cap keeps prolonged boss farming from
exceeding the Maximum column.

| Sector | Cautious 45% / 65% | Expected 65% / 85% | Skilled 90% / 100% | Maximum |
|---|---:|---:|---:|---:|
| 1 Blue Coast | 208 | 272 | 344 | 369 |
| 2 Coral Gardens | 166 | 211 | 261 | 277 |
| 3 Ghost Nets | 191 | 240 | 293 | 309 |
| 4 Sunken City | 186 | 232 | 286 | 307 |
| 5 Black Tide | 205 | 253 | 312 | 335 |
| 6 Silent Reef | 236 | 293 | 363 | 390 |
| 7 Frozen Depths | 253 | 314 | 389 | 419 |
| 8 Abyss Mine | 291 | 364 | 454 | 490 |
| 9 Plastic Vortex | 298 | 368 | 452 | 484 |
| 10 NEREID Core | 284 | 328 | 381 | 401 |
| **Campaign** | **2,318** | **2,875** | **3,535** | **3,781** |

The six upgrade tracks cost 4,980 salvage in total. A cheapest-available purchase simulation gives
21 ranks to the cautious model, 23 to the expected model, 25 to the skilled model, and 26 to the
theoretical maximum. Actual choices may produce fewer ranks when a player specializes in expensive
late tiers; the purchased power remains comparable because each track uses increasing cumulative
effects.

| Upgrade | Rank costs | Total |
|---|---|---:|
| Primary Weapon | 60, 120, 180, 270, 360 | 990 |
| Hull | 45, 90, 135, 200, 280 | 750 |
| Cleanup Beam | 35, 70, 110, 165, 240 | 620 |
| Shield | 50, 100, 150, 225, 320 | 845 |
| Rescue System | 35, 70, 110, 165, 240 | 620 |
| Support Drone | 70, 140, 210, 315, 420 | 1,155 |

`CampaignBalanceTest` recalculates these figures from shipped config. It fails if a perfect Normal
campaign can buy all upgrades or if cautious no-ad progression falls below the intended range.

## Equipment roles

TIDE is the baseline and has the highest sustained primary damage at about 55.6 before weapon
multipliers. MANTA trades 15 hull and about 10% primary output for 20% movement and cleanup power.
LEVIATHAN has about 50 sustained primary damage, 140 hull, and 20 shield, but loses 25% movement.
This keeps the heavy vessel durable without also making it the fastest boss-clear choice.

Weapon ideal-output multipliers stay within 0.95-1.10 of Pulse Cannon. Spread Cannon can reach 1.08
only when all three lanes connect. Focus Laser reaches about 1.02 with lane restrictions, Homing
Micro-Torpedo reaches 0.99 in exchange for tracking, and Support Drone reaches about 0.98 from its
offset firing position. Kaia, Atlas, Neri, and Rook each own one distinct 10-20% specialty in cleanup,
hull, rescue, or damage.

## Difficulty and teaching curve

Difficulty never scales health alone. `config/campaign.properties` raises enemy health, projectile
speed, spawn density, fire rate, boss cadence, projectile count, movement, and phase pressure.
Mission systems add their own difficulty effects, including tighter routes, longer concealment,
greater sonar cost, faster heat or pressure gain, stronger vortex currents, denser boss patterns,
and shorter warnings.

The campaign introduces systems in this order:

1. Blue Coast teaches movement, auto-fire, cleanup, rescue, salvage, and a telegraphed boss.
2. Coral Gardens and Ghost Nets add protected habitat, armor, currents, nets, and timed wildlife.
3. Sunken City and Black Tide add destructible hazards, visibility, oil cleanup, and valves.
4. Silent Reef and Frozen Depths add an active sonar resource and thermal safe zones.
5. Abyss Mine and Plastic Vortex combine pressure, sonar, moving routes, currents, and cleanup combos.
6. NEREID Core combines previously taught patterns and requires combat, cleanup, rescue, sonar, and
   a final escape before campaign completion.

Automated tests validate formulas, progression, and deterministic systems. Device playtesting is
still required for touch comfort, perceived boss pressure, readability on physical cutouts, and the
time real players need to collect salvage drops.
