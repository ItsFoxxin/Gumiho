# GUMIHO Link relay 0.9.5-link.3

This is the standalone relay for GUMIHO Link. It is not a Minecraft server mod and
does not belong in any `mods` directory.

For the production PowerEdge target, start with
[`deploy/proxmox-lxc/README.md`](deploy/proxmox-lxc/README.md), then keep
[`OPERATIONS.md`](OPERATIONS.md) with the service for health checks, backups,
recovery, updates and rollback.

The intended boundary is:

    Internet -> TCP 443 -> Caddy in unprivileged LXC -> 127.0.0.1:8791 -> Node relay

Never forward or expose port 8791. The installer refuses Node versions below 24,
preserves an existing config/bootstrap credential, retains an adjacent original
systemd unit when present, and deliberately does not start the service.

Run the dependency-free test suite before installation:

    node --test

Expected result for this package: 31 passed, 0 failed.
