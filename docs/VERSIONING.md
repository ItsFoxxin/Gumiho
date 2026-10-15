# Versioning and branch policy

GUMIHO uses [Semantic Versioning](https://semver.org/) for the client builds and Link relay.

`VERSION` is the canonical version. The same value must appear in:

- `clients/mc1.21.1/gradle.properties`
- `clients/mc26.2/gradle.properties`
- `relay/package.json`

CI rejects a mismatch. Change every declaration safely with:

```bash
node scripts/set-version.mjs 1.0.0-rc.1
node scripts/check-version.mjs
```

## Version meanings

- Patch: compatible fixes, such as `0.9.5` to `0.9.6`.
- Minor: compatible feature batches, such as `0.9.6` to `0.10.0`.
- Major: compatibility or protocol breaks, such as `1.0.0` to `2.0.0`.
- Prerelease: test candidates, such as `1.0.0-alpha.1`, `1.0.0-beta.1`, or `1.0.0-rc.1`.

The current `0.9.5-link.3` value is a valid SemVer prerelease. Stable tags use `v<version>`, for example `v1.0.0`.

## Branch layers

- `dev`: integration branch. Every push compiles both Minecraft targets, runs all client regression harnesses, runs relay tests, and validates repository/version policy.
- `master`: stable branch. Pull requests and pushes receive a clean build plus all regression checks and Java-to-relay integration. A push to `master` also creates versioned full-client, Link-only, and relay artifacts with SHA-256 manifests.

Do not commit generated JARs. GitHub Actions builds them from the reviewed source tree.
