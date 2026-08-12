# App List Icon Explorations

These boards explore 24 initial icon-family directions and three production-oriented color directions for the MiniIVI app list. Every family covers the same eight functions: Media, Video, Weather, Browser, Bluetooth, Maps, Phone, and Settings.

- `A01`-`A06`: precision automotive and technical line systems
- `B01`-`B06`: restrained premium material treatments
- `C01`-`C06`: minimal vector systems optimized for small sizes
- `D01`-`D06`: restrained futuristic automotive geometry
- `E01`: familiar adaptive app tiles with strong functional color coding
- `E02`: dark premium containers with colored glyphs and restrained signal rings
- `E03`: low-glare two-tone tiles optimized for fast recognition at night

## Colored production directions

- `app-icons-board-e01-oem-adaptive.png`: the strongest default candidate. It follows the familiar colored-app model used across contemporary automotive app launchers while keeping every asset original.
- `app-icons-board-e02-premium-signal.png`: a more distinctive premium direction with higher visual energy.
- `app-icons-board-e03-soft-two-tone.png`: the calmest direction and the most straightforward one to reproduce as deterministic vectors.

The color mapping is intentionally stable across the three directions: warm coral or orange for Media, red or magenta for Video, cyan and amber for Weather, blue for Browser and Bluetooth, teal with a warm locator accent for Maps, green for Phone, and violet for Settings. Shape remains the primary identifier; color is a redundant recognition cue rather than the only differentiator.

Recommended implementation candidates:

- `A03`: strongest visual continuity with the current SystemUI language
- `B05`: premium and highly legible without relying on a container
- `C05`: simplest colored-accent system with clear app recognition
- `C06`: clearest app-tile identity and straightforward vector implementation
- `D02`: distinctive technical framing while remaining practical
- `E01`: best overall balance of familiarity, glanceability, and production practicality
- `E03`: best low-glare alternative for a calmer cabin theme

These are selection concepts, not production icon sources. After a family is selected, redraw the eight glyphs on a shared vector grid and validate them at the actual 24-32 dp render sizes.
