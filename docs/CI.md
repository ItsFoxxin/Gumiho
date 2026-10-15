# CI gates

## Development CI

Triggers on pushes and pull requests to `dev`.

1. Validate Semantic Versioning and declaration consistency.
2. Reject generated JARs, logs, private relay state, environment files, and synchronization-conflict copies.
3. Test the dependency-free Node 24 relay.
4. Compile Minecraft 1.21.1 with Java 21 and run every executable regression suite.
5. Compile Minecraft 26.2 with Java 25 and run every executable regression suite, including spear coverage.

This is the quick integration layer. It intentionally does not publish downloadable game artifacts.

## Stable Release CI

Triggers on pull requests and pushes to `main`, and can be run manually.

It repeats policy checks, then performs clean client builds, every regression suite, the Java-to-Node Link fixture, and JAR-content inspection. The Link-only JAR is rejected if it accidentally contains the full client's modules or assets.

On a `main` push (or a manual run), CI uploads:

- full GUMIHO and Link-only JARs for Minecraft 1.21.1;
- full GUMIHO and Link-only JARs for Minecraft 26.2;
- the Link relay package;
- build metadata and SHA-256 checksum manifests.

Pull requests run the complete gate but do not publish downloadable artifacts.
