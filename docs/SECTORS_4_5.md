# Sunken City and Black Tide

Both missions use the shared `GameScreen`, `GameWorld`, `SpawnTimeline`, enemy component maps,
and profile progression. Their waves, environment events, creatures, boss tuning, salvage rewards,
and five-to-seven-minute timing are authored in JSON.

## Sunken City

Sunken City follows a moving corridor through NEREID's drowned waste district. Ruins and chemical
barrels can be destroyed or avoided. Damaging a barrel releases a temporary toxic field. Unstable
structures show a warning before collapsing, and research divers use the existing continuous rescue
interaction. Toxic fields and the midpoint event temporarily reduce visibility.

Chemical Bomber, Ruin Turret, Salvage Mech, and Ambush Drone combine reusable movement, weapon,
armor, support, concealment, and hazard behaviors. Urban Salvager begins with scrap volleys and
turret support, builds two metal armor plates, then exposes its energy core after both plates break.

## Black Tide

Black Tide uses reusable oil fields, cleanup capsules, and valve interactions. Oil reduces the visible
area and can be removed by the cleanup beam or by collecting an absorbent capsule. Authored leak
valves close through the same cleanup interaction. Enemies emerging from the oil remain concealed
briefly, with longer concealment on higher difficulties.

Oil Spreader, Ignition Drone, Pressure Tanker, and Pipeline Guard share the same enemy systems.
Oil Kraken attacks with pipe arms and oil spray. Its second phase requires both pressure valves to be
closed and at least 60% of its boss oil to be cleaned. The exposed core phase accelerates as visibility
returns.

## Difficulty and progression

Normal, Hard, Expert, and Abyss retain the campaign health, density, projectile, fire-rate, movement,
and boss-cadence multipliers. The environment system additionally shortens collapse warnings,
increases hazard frequency, extends oil concealment, narrows Sunken City's route, and increases
cleanup/valve interaction time. Clearing Sunken City opens Black Tide and awards `City of Light`.
Clearing Black Tide opens the stable Sector 6 record and awards `Break the Black Tide`. Boss and enemy
salvage values come from each mission config.

## Manual validation

1. Clear Ghost Nets with at least one star and launch Sunken City. Confirm the rust, dark-blue, and
   toxic-green palette, moving corridor, four enemy types, diver rescues, and temporary visibility loss.
2. Shoot a chemical barrel once. Confirm a toxic field appears; clean it with the cleanup beam. Avoid
   one ruin, destroy another, and confirm a collapsing structure displays its warning first.
3. Reach Urban Salvager. Verify scrap and turret support, immunity while metal plates are active, and
   core damage only after both plates break. Complete recovery and confirm salvage, achievement, and
   Black Tide unlock.
4. In Black Tide, enter and leave an oil field, clean another with the beam, and collect a cleanup
   capsule near a separate field. Close an authored leak valve and rescue both diver types.
5. Confirm oil contacts remain hidden briefly and Ignition Drone can ignite a slick. Reach Oil Kraken,
   close both boss valves, and verify the core stays protected below 60% boss-oil cleanup.
6. Clean enough oil, defeat the accelerating core phase, and confirm the before/after report,
   achievement, salvage, and Sector 6 record unlock.
7. Repeat representative encounters on Hard, Expert, and Abyss. Confirm increased enemy density and
   attack cadence, narrower routes, shorter collapse warnings, longer concealment, and slower hazard
   interactions.

All new visuals are original procedural geometry in `OceanRenderer`; no external assets were added.
