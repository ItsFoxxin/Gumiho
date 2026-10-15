# Roadmap to 1.0.0 - paired Minecraft builds

Updated 2026-08-30. Current implementation: 0.9.0 test builds for Minecraft 1.21.1 and 26.2 / NeoForge.
From **0.8.9 onward, keep BOTH 1.21.1 and 26.2 supported**. The newer port does not
replace or retire the 1.21.1 client. Plans below are not claims of implemented features.

- **0.8.4:** close out the current targeting/melee pass with mace combo aiming and reactive
  shields. Validate this test build before starting the next feature category.
- **0.8.5 base:** begin the ranged pass on the existing Minecraft 1.21.1 / NeoForge baseline.
  Started: Utility Projectile Paths for supported in-flight projectiles, prediction settings,
  and estimated impact markers. See [0.8.5](0.8.5.md) for scope and validation.
  Ranged weapons continue in 0.8.6; held-item previews and a full ranged review
  were not included in this first build.
- **0.8.6 ranged weapons:** shared bow/crossbow/thrown-trident aim, separate target
  categories, configurable easing/lead/path checking, loaded-crossbow hold key, and
  one ranged status HUD. Crossbow/trident opt-in; firing manual. Test this build
  before expanding release automation, water prediction or held-item previews.
- **0.8.7 combat follow-up:** reset-aware mace targeting, landing guidance/optional
  air-input correction, broader independent shield defense, and held projectile
  previews. Validate against the supplied recordings' failure cases before further expansion.
- **0.8.8 shield follow-up:** manual attack handoff and bounded full-cooldown automatic
  openings, with reblock recovery and predicted-impact priority. Validate the 1.21.1
  test build before treating it as the port baseline. See [0.8.8](0.8.8.md).
- **26.2 port baseline, before 0.8.9 acceptance:** create a separate build/profile;
  preserve the working 1.21.1 source and release artifacts. Verify the target JDK,
  NeoForge/build-plugin versions, native APIs and modpack before enabling automation.
  Compile and test the existing combat, HUD, input, projectile, config and safety
  behavior on the new game version. Do not merely change the version metadata.
- **0.8.9 combat compatibility and spears, TWO builds:** add native spear support on
  26.2 and audit missing/changed weapon behavior, cooldowns, reach, damage conditions,
  shield interactions, aiming and related HUD feedback. Scope each newly identified
  mechanic explicitly; do not assume existing sword/mace rules apply to it.
  Ship shared fixes on 1.21.1 too. Native spear functionality is unavailable on vanilla
  1.21.1; do not show nonfunctional toggles or fabricate a client-only weapon backport.
  Any modded 1.21.1 spear integration would need that mod identified and tested separately.
- **Movement/elytra pass:** still planned after newer-version combat validation, but
  no longer assigned 0.8.9. Confirm its revised release slot before implementation.
- **0.9.0:** Visual/Utility test release: Network HUD recovery, responsive controls,
  combat grouping/help, Compass/Equipment HUDs, cached resource/effect panels and
  render-only precipitation hiding. Also includes requested Bridge/Tower/Wall building,
  full-height Mace HUD accents, named kit saves and clipboard HUD transfer.
  See [release guide](0.9.0.md).
  Builds pass; live UI/weather-hook/modpack acceptance remains pending.
- **0.9.5:** settings confirmation/cleanup.
- **0.9.9:** pre-release staging and validation for 1.0.0.
- **1.0.0:** base release, including the separately planned client-only GUMIHO Link direction.

## Dual-version release rules

- One GUMIHO feature/release number, two version-labeled JARs:
  `gumiho-neoforge-0.9.0-mc1.21.1.jar` and `gumiho-neoforge-0.9.0-mc26.2.jar`.
  See [0.9.0](0.9.0.md) for compatibility and acceptance tests.
- Match each artifact's metadata to its Minecraft/NeoForge target; install only the
  matching JAR in that profile. A single JAR is not advertised as cross-version.
- Reuse game-independent rules/tests where practical; isolate version-specific input,
  item/combat and rendering adapters. Keep behavior aligned without forcing false parity.
- Maintain separate run directories, config copies and regression/smoke-test results.
  Back up configs before migrations; preserve toggles, player lists and allowlists.
- Validate both targets for each paired release, including manual overrides, full-charge
  attacks, shield startup, target protection, GUI/HUD layout and projectile previews.
  Report per-target failures and pending in-game checks rather than marking both verified.
- Recheck Connector/FFAPI, SeedCrackerX, Iris and Sodium independently for each target;
  the present 1.21.1 combination is not proof of 26.2 support. Never auto-upgrade the
  server/world/profile as part of building a client JAR.

## Target verification

Checked 2026-08-30: Mojang's launcher manifest identifies **26.2** as the latest
release (26.3-snapshot-10 is a snapshot, not the selected target). NeoForge Maven
metadata lists 26.2 builds through 26.2.0.75 at this check. Recheck and pin a tested
build at port time rather than depending on a floating latest version.

- [Minecraft Java Edition 26.2 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2)
- [Mojang launcher version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
- [NeoForge project listing](https://projects.neoforged.net/neoforged/neoforge/)
- [NeoForge Maven version metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)

The 0.8.9 baseline includes the native 26.2 port and opt-in spear assist/status. Elytra automation remains planned.
Preserve the client-only design, explicit module toggles and private-server allowlist.
GUMIHO Link remains separately planned client-only team coordination, with hold-to-view
UI and role-controlled sharing; its implementation/infrastructure needs its own pass.
