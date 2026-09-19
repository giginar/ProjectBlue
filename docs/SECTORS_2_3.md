# Coral Gardens and Ghost Nets

Both missions use the existing `GameScreen`, pure Java `GameWorld`, fixed entity pools, and
density-adjusted `SpawnTimeline`. Their configuration lives in `coral-gardens.json` and
`ghost-nets.json`; no downloaded assets are required because all new placeholder art is drawn by
`OceanRenderer`.

## Coral Gardens

Coral Gardens runs for about five to six minutes and introduces Coral Cutter, Shield Carrier, and
Burrow Drone waves. Protected coral accepts player and enemy damage while rescue creatures do not
accept weapon damage. Cleanup gradually restores turquoise and coral-red color to the initially
bleached reef. The rescued species are seahorses, mantas, and reef fish schools.

The Reef Breaker has three distinct phases. Cutter arms first constrain the safe lane while support
drones enter. At two-thirds health its core closes and two shield generators must be destroyed.
The final exposed core periodically warns before striking protected coral. Boss cadence, movement,
projectile count, health, and support density scale through Normal, Hard, Expert, and Abyss.

## Ghost Nets

Ghost Nets runs for about five to six minutes in darker water with moving currents. Nets slow the
submarine on contact. Standard weapon hits open them gradually, while the reusable
`NetCuttingSystem` makes the proximity cutter faster. Currents move nets, debris, and trapped
wildlife. Timed rescue signals can leave the playfield and reduce the Rescue result without showing
a death animation. The rescued species are sea turtles, seals, and fish schools.

The Ghost Net Harvester begins with large nets and Net Launcher support. Its second phase moves a
narrow safe channel between net walls. At one-third health the core closes until both net
generators are disabled; the center core then opens for the final phase.

## Progression and scoring

Combat uses enemies actually encountered. Cleanup includes authored waste and habitat damage;
Rescue uses configured creature signals; Integrity combines remaining hull with protected habitat
health. Both missions award configured salvage, update the existing achievements, and use the
shared 0-3 star calculation. Clearing Coral Gardens opens Ghost Nets. Clearing Ghost Nets opens the
playable Sunken City mission. Profile schema
v3 is unchanged.

## Manual checks

1. Clear Blue Coast and launch Coral Gardens from Level Select. Confirm the intro objective, all
   three enemy types, the midpoint convoy message, and the turquoise/coral/gray palette.
2. Shoot protected coral and confirm its health, Cleanup, and Integrity fall. Confirm shots pass
   through seahorses, mantas, and reef fish without damaging them.
3. Let a Coral Cutter reach coral and confirm it damages the habitat. Confirm a Shield Carrier
   briefly shields nearby enemies and a Burrow Drone emerges near coral before attacking.
4. At the Reef Breaker, confirm cutter and coral-strike warnings appear before hazards. Confirm the
   core rejects damage until both phase-two shield generators are destroyed.
5. Clear Coral Gardens and launch Ghost Nets. Confirm current drift, contact slowdown, the midpoint
   message, and the navy/green/pale palette.
6. Compare standard fire with the proximity cutter on nets. Let a timed rescue signal expire and
   confirm Rescue falls without a death animation.
7. At the Ghost Net Harvester, confirm large net attacks, the moving safe channel, generator-gated
   core, and all warning states. Complete the mission and confirm Sector 4 is unlocked but cannot
   launch.
8. Repeat representative runs on Hard, Expert, and Abyss. Confirm wave density, health, shot speed,
   firing rate, boss cadence, projectile count, and movement increase.
