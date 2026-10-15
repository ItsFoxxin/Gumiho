# GUMIHO 0.9.5-link.3 — member-management test candidate

Current candidate: [installation, keys, member management, local relay setup and tests](docs/0.9.5-link.3.md).
Built on the accepted 0.9.2 baseline. Not final 1.0; elytras/fishing/crystal PvP remain deferred.


Native NeoForge client mod for Minecraft Java Edition 26.2, built for private, consent-based servers.

## Active 1.0 integration branch

Based on the accepted 0.9.2 source release. GUMIHO Link is the remaining new feature.
Elytras, fishing and crystal PvP are deferred until after 1.0. See [scope and gates](docs/1.0.0-PLAN.md).
Do not call this a 1.0 release until Link integration and live acceptance pass.

## Current test baseline

**0.9.2** unifies HUD background color/opacity, removes double rendering in the editor, makes Survival orange, and boxes coordinates. Your positions, sizes and presets are preserved. See [0.9.2 changes and test checklist](docs/0.9.2.md).
Both builds and logic tests pass; in-game acceptance remains pending. Previous 0.9.1 artifacts remain available for rollback.

## Compatibility

- Minecraft 26.2
- NeoForge 26.2.0.75 (build/test baseline)
- Java 25
- Client-side only; do not place the GUMIHO JAR in the dedicated server's `mods` folder
- Native NeoForge mod. Compatibility with Connector/FFAPI, SeedCrackerX, Iris and Sodium must be checked separately for 26.2; do not reuse their 1.21.1 JARs.

## Build

```powershell
.\gradlew.bat build
```

The JAR is created in `build\libs`.

## Use

Install the built JAR in the CurseForge profile's `mods` folder. Press Left Shift in game to open `GUMIHO | 구미호`. The binding can also be changed in Minecraft's Controls menu.

Create a separate Minecraft 26.2 / NeoForge 26.2.0.75 profile with Java 25. Install `gumiho-neoforge-0.9.2-mc26.2.jar` there, with only one GUMIHO JAR enabled. Keep your 1.21.1 profile and worlds intact. Do not upgrade a live world just to test this port.

GUMIHO itself does not need a Fabric compatibility layer or a dedicated-server installation. The client and server must use compatible game/modpack versions; this mod does not translate a 26.2 client into a 1.21.1 client.

The first run creates `config\gumiho.json`. Protected aim modules only activate in single-player or on an address explicitly listed in `allowedServers`.

To authorize a multiplayer server, close Minecraft and edit the CurseForge instance's `config\gumiho.json`. Add the address exactly as it appears in Minecraft's server list, including a custom port when one is used:

```json
"allowedServers": [
  "localhost",
  "127.0.0.1",
  "play.example.net",
  "192.168.1.50:25570"
]
```

Restart Minecraft after saving. Capitalization does not matter, but the hostname/IP and port must match the address used to connect.

## 0.8.9 paired-version combat baseline

See [0.8.9 setup, limitations and test checklist](docs/0.8.9.md).

- Separate native 1.21.1 and 26.2 builds, each marked with its game version
- Held tridents now work with melee aim/cooldown attacks and are preserved by Auto Weapon
- 26.2 only: opt-in Spear Assist, native cooldown jabs, manual charge aim and Spear Status HUD
- 26.2 HUD/input/projectile rendering ported to the new native APIs
- 1.21.1 does not contain spear modules; no missing-weapon toggles or backported items
- Multiplayer/modpack combat acceptance tests remain required; this is a test release

## 0.8.8 shield attack windows

See [0.8.8 setup and tests](docs/0.8.8.md) and the [dual-version roadmap](docs/ROADMAP.md).

- Manual attack presses briefly release only an automatically raised shield
- Automatic openings require a valid target and full native attack cooldown
- One hit per opening; defense reevaluates next tick, with normal shield startup
- Predicted imminent projectile/explosion impacts defer automatic openings
- Combat Options page 3/5 exposes the existing Shield Attack Windows setting
- Starting with 0.8.9, maintain separate 1.21.1 and 26.2 builds; install the matching JAR only

## 0.8.7 reset-aware mace, expanded shield defense and held previews

See [0.8.7 setup, limitations and tests](docs/0.8.7.md).

- Mace Combo Assist replaces permanent locks with reachable landing/reset opportunities
- Orange landing / green target rings, directional HUD and opt-in normal-input air correction
- Auto Shield remains independent of auto-melee; close melee and explosion guard settings added
- More projectile threats recognized; five-tick shield startup and bypass damage still apply
- Projectile Paths previews charged/held weapons and supported throwables before firing
- Both legacy toggles/positions and in-flight player/mob paths are preserved
- Older release JARs/source archives remain available for rollback

## 0.8.6 ranged weapons and combat

See [0.8.6 setup and acceptance tests](docs/0.8.6.md).

- Mob Ranged Aim / Player Ranged Aim retain their old toggles, now sharing one controller
- Bow, arrow-crossbow and thrown-trident prediction; crossbow/trident support opt-in
- Hold V (remappable) to aim a loaded crossbow; all firing remains manual
- Separate weapon settings, target lead, flight-path checks, FOV, speed and easing
- Ranged Status HUD replaces the bow-only panel while keeping its position
- Riptide and firework crossbows stay manual; private-server/friend protections retained
- Utility Projectile Paths still covers supported shots from you, other players and mobs

## 0.8.5 ranged baseline: Projectile Paths

See [0.8.5 setup, limits and test checklist](docs/0.8.5.md).

- New OFF-by-default Utility > Projectile Paths for supported projectiles already in flight
- Ghast/blaze/dragon fireballs, wither skulls, arrows, outbound tridents, wind charges and throwable items
- Color-coded, depth-tested paths with estimated block/entity contact markers
- Right-click the card for range, prediction length, path count, owner filters and marker options
- Separate gravity/drag/acceleration profiles, loaded-world bounds and a shared simulation budget
- Existing melee behavior unchanged; held-item previews and the newer-version port are not included yet

## 0.8.4 melee closeout: combo lock and reactive shields

See [0.8.4 setup and testing](docs/0.8.4.md) and the [next milestones](docs/ROADMAP.md).

- New OFF-by-default Mace Combo Lock keeps aiming at one visible eligible target through bounces
- Tracking range defaults to 32m; actual attacks still require normal melee reach and full charge
- Target / Intel HUD follows the combo target and displays MACE LOCK
- Auto Shield uses predicted impact windows instead of idle archer or proximity holding
- Manual Use/building releases only the module's own block and takes priority
- Page 4 settings: Mace Lock Range, Shield Lead, and optional React to Swings

## 0.8.3 responsive aim, projectile shielding, and wind-charge swap

See [the 0.8.3 guide](docs/0.8.3.md) for setup and focused tests.

- Melee defaults now 6 degrees/tick maximum (120 degrees/sec), 80 ms easing; frame-based smoothing and target stability retained
- Auto Shield holds through arrow threats instead of repeatedly dropping; anticipates frontal archers and includes arrow drop in prediction
- Incoming-arrow defense no longer depends on the shooter's enemy-list status
- New OFF-by-default Wind Charge → Mace toggle: manually throw a mainhand wind charge, then automatically select a hotbar mace
- Fast swap does not bypass the native mace recharge. Mainhand mace + offhand charges avoids switching the mainhand item
- Old default aim values upgrade once; customized values and all module toggles are preserved

## 0.8.2 smooth melee, Auto Shield, and mace compatibility

See [the 0.8.2 guide and live test checklist](docs/0.8.2.md).

- Render-frame melee aiming with speed-capped easing and stable target selection
- New Auto Shield toggle, OFF by default, for an equipped offhand shield
- Combat Options page 4: aim easing, shield range/hold, projectile checks, and attack windows
- Fresh attack-time raycasts for fast movement and mace descents
- No late cooldown reset when a previously switched weapon has already charged
- Food/shield automation yields during wind-charge use and airborne Mace Smash Timing
- Vanilla mace damage, Density/Breach/Wind Burst, and fall-damage rules remain server-controlled

No automatic wind-charge throw, jump, clutch, or fall-damage bypass is included. Wind Burst can
leave you exposed to fall damage after the bounce. Confirm your exact modpack in a test world.

## 0.8.0 targeting and melee foundation

See [the 0.8.0 guide and test checklist](docs/0.8.0.md) for settings, limitations, and upgrade instructions.

- Shared target filters, full FOV, range, and crosshair/distance/health priority
- Optional Melee Aim for swords, axes, and maces (aim only)
- Optional Auto Weapon: prefer a sword, choose an axe against a shield, preserve a manually selected mace
- Optional Mace Smash Timing: fully charged crosshair attacks while descending
- Mace Status: fall distance, readiness, and unenchanted fall-bonus estimate
- One Target / Intel HUD with compact/detailed modes, shield state, equipment, and approximate hurt-animation time
- Combat Options button and right-click combat-card settings (now four pages)
- Shared attack guard preserving the 0.7.2 timer reset and same-tick duplicate protection
- Existing config is backed up to gumiho.pre-0.8.0.json before merging the retired Enemy Intel panel

New automation starts OFF and is not silently added to existing combat presets. Enable each new module explicitly to test it. Release the manual attack button when testing Cooldown Attack or Mace Smash Timing; vanilla manual clicks are not intercepted.

## Existing modules retained

- Auto Sprint and Auto Walk
- Fullbright (maximum client brightness)
- No Weather (client-side rain and thunder suppression)
- Brand and Coordinates HUD elements
- Separate Mob Bow Aim and Player Bow Aim toggles; both lead visible moving targets while drawing
- Bow charge and arrow-count HUD
- Target / Intel HUD for eligible visible mobs and marked enemy players
- Melee attack cooldown HUD
- Auto Totem at five hearts or lower
- Auto Eat using the highest-nutrition food in the hotbar below seven hunger bars
- Cooldown Attack for the living target directly under the crosshair
- Bridge Assist with edge crouching and normal held-block placement
- Draggable Player Tracker HUD with teammate, enemy, and neutral colors
- Weak-enemy and low-teammate warnings, distance, health, and armor condition
- Persistent player groups that protect teammates from Player Bow Aim and Cooldown Attack
- Five cheat profiles: Custom, Assist, Enhanced, Aggressive, and War
- Threat scores and relative direction markers in the Player Tracker
- Screen-edge indicators for loaded marked enemies
- Enemy Intel merged into the detailed Target / Intel panel (no duplicate module)
- Incoming-projectile direction and estimated impact-time warning
- Survival alerts for low health, weak armor, missing totems, and incoming-hit direction
- Potion effect timers plus pearl and shield cooldown indicators
- Optional Combat Resources counter and Network HUD utilities
- Settings toggles for contextual HUD hiding, neutral-player visibility, and weak-enemy priority
- Context-aware HUD panels that disappear when they have nothing useful to report
- Scrollable module pages with clipped descriptions that no longer run under toggles
- Bottom HUD-editor toolbar, full access to the top edge, and Reset Layout
- Settings sliders for bow range/smoothing, Auto Totem health, Auto Eat hunger, and health warnings
- In-menu player list editor for teammates, enemies, neutral players, and pinned targets
- Middle-click enemy pinning with automatic target priority; rebindable in Controls
- End-key emergency stop that disables protected automation while preserving informational HUDs
- Draggable HUD editor with persistent positions
- Category sidebar, live module search, bilingual descriptions, and custom orange toggles

Auto Totem, Auto Eat, Auto Shield, Cooldown Attack, Bridge Assist, Mob Bow Aim, Player Bow Aim, Melee Aim, Auto Weapon, and Mace Smash Timing are protected by the server allowlist. They stop running whenever the connected address is not authorized. The End-key emergency stop disables them.

## Player lists

These are client-side commands and do not require server permissions:

```text
/gumiho team add PlayerName
/gumiho enemy add PlayerName
/gumiho neutral PlayerName
/gumiho remove PlayerName
/gumiho list
/gumiho profile assist
/gumiho profile enhanced
/gumiho profile aggressive
/gumiho profile war
/gumiho profile custom
```

Unlisted players are neutral. Player Bow Aim and Cooldown Attack only attack players explicitly saved as enemies. The tracker can only report players currently loaded by the client; it cannot locate offline players or players outside the client's received world data.

The main-menu Setups button opens named kit/layout saves and clipboard HUD transfer. In its Kit tab, the built-in profile button cycles through Custom, Assist, Enhanced, Aggressive, and War. Manually changing any module returns the profile to Custom. Protected automation still obeys `allowedServers` regardless of profile.
