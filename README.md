# GUMIHO | 구미호

GUMIHO is a client-side NeoForge combat, awareness, movement, and team-coordination mod for private, consent-based Minecraft servers. This repository contains both supported game targets and the optional GUMIHO Link relay.

Current version: **0.9.5-link.3** (pre-1.0 test candidate).

## Repository layout

- `clients/mc1.21.1`: NeoForge client for Minecraft 1.21.1 / Java 21.
- `clients/mc26.2`: NeoForge client for Minecraft 26.2 / Java 25.
- `relay`: dependency-free Node 24 GUMIHO Link relay and its Proxmox LXC deployment files.
- `docs`: protocol, client testing, CI, release, and versioning documentation.
- `scripts`: version, repository-policy, and built-JAR verification tools.

The full client and Link-only client are separate artifacts. Install one or the other—never both in the same Minecraft profile. Neither client JAR belongs in the dedicated server's `mods` folder.

## Build locally

Minecraft 1.21.1:

```powershell
cd clients\mc1.21.1
.\gradlew.bat clean check assemble
```

Minecraft 26.2:

```powershell
cd clients\mc26.2
.\gradlew.bat clean check assemble
```

Relay:

```powershell
cd relay
npm test
```

## Branch and release flow

- Work lands on `dev`, where GitHub Actions validates version consistency, the relay, and both Minecraft clients.
- A pull request from `dev` to `main` runs the stricter clean-build and cross-runtime integration gate.
- A successful push to `main` produces versioned JAR/relay artifacts and SHA-256 manifests.

See [CI gates](docs/CI.md) and [versioning policy](docs/VERSIONING.md).

## Safety and privacy

Protected automation is restricted to single-player or explicitly allowlisted servers. GUMIHO Link is client-only and role-aware; location/stat sharing remains off until the user enables it. Never commit relay bootstrap keys, member keys, runtime state, `.env` files, or populated configuration secrets.

## Current release status

The source and automated suites cover both clients and the relay. The candidate still needs live two-client game acceptance against the deployed relay before it should be called 1.0.0.
