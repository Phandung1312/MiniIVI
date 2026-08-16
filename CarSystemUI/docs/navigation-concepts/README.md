# Navigation rail concepts

These mockups use `tmp/dark-glass-home-final.png` as the visual reference. They preserve the current 135.2 dp rail footprint and explore useful ways to replace its unused vertical space.

## A. Balanced Dock

`navigation-rail-a-balanced-dock.png`

- Adds direct Home, Apps, Maps, Media, and Phone destinations.
- Uses a compact battery and cabin-temperature tile to bridge navigation and vehicle context.
- Keeps Quick Controls and Settings anchored at the bottom.
- Best general-purpose direction when fast app access is the priority.

## B. Driver Glance

`navigation-rail-b-driver-glance.png`

- Keeps the primary navigation set deliberately small.
- Uses the center area for cabin temperature, battery, and Bluetooth connection state.
- Keeps Maps, Quick Controls, and Settings within easy reach.
- Best direction for low-distraction, at-a-glance driving information.

## C. Media Aware

`navigation-rail-c-media-aware.png`

- Keeps Home, Apps, and Maps at the top.
- Uses the center area for compact previous, play/pause, next, and progress controls.
- Adds a connected Bluetooth shortcut above Quick Controls and Settings.
- Best direction when media is the most frequent in-drive interaction.

## Shared generation brief

- Use case: `precise-object-edit`.
- Asset type: high-fidelity Android Automotive SystemUI navigation rail mockup.
- Edit target: `tmp/dark-glass-home-final.png`.
- Change only the left navigation rail and preserve the dashboard content to its right.
- Match the existing dark translucent glass, aubergine surfaces, lavender active state, soft pink accent, translucent white borders, subtle shadows, and 22 px rounded geometry.
- Keep controls legible at driving distance with large touch targets.
- Avoid neon, chrome, decorative telemetry, tiny controls, and UI outside the rail.

The three variant-specific prompts added only the layout and feature choices documented above.
