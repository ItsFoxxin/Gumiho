# GUMIHO Link relay operations

This package targets an unprivileged Proxmox LXC with Node.js 24+ and Caddy. It is a
separate service; no JAR belongs in the Minecraft server's `mods` directory.

## Files and trust boundary

- `/opt/gumiho-link/relay`: immutable application code, owned by root.
- `/etc/gumiho-link/relay.json`: paths, port and exact public hostname; no credential.
- `/var/lib/gumiho-link/private-bootstrap.json`: team-creation credential, mode 0600.
- `/var/lib/gumiho-link/private-membership.json`: member token hashes and roles, mode 0600.
- Live coordinates, stats and pings are memory-only and disappear on restart.
- Node listens only on `127.0.0.1`; Caddy is the TLS/public boundary.

## First start

Edit `/etc/gumiho-link/relay.json` and Caddy's site so their hostnames match exactly.
Then:

    systemctl enable --now caddy
    systemctl enable --now gumiho-link
    systemctl status gumiho-link --no-pager
    journalctl -u gumiho-link -n 50 --no-pager
    node /opt/gumiho-link/relay/src/healthcheck.mjs /etc/gumiho-link/relay.json
    curl --fail --silent --show-error https://link.example.com/health

The health response contains only `{ "ok": true, "protocol": 1 }`.
Set the client's endpoint to `https://link.example.com`. Only the owner reads the
bootstrap file to create the first team. Never send that bootstrap key to members.

## Backups

Create a root-only destination and run:

    install -d -m 0700 /var/backups/gumiho-link/snapshots
    node /opt/gumiho-link/relay/src/backup.mjs create \
      /etc/gumiho-link/relay.json /var/backups/gumiho-link/snapshots

Verify any backup before relying on it:

    node /opt/gumiho-link/relay/src/backup.mjs verify \
      /var/backups/gumiho-link/snapshots/<backup-directory>

Backups contain the bootstrap secret and member token hashes. Encrypt them off-host and
do not place them in a normal shared Proxmox backup store without access controls.

## Restore

1. Verify the selected backup.
2. Stop the service: `systemctl stop gumiho-link`.
3. Create a fresh backup of the current `/var/lib/gumiho-link` directory before replacing anything.
4. Copy the verified `private-bootstrap.json` and optional `private-membership.json` into
   `/var/lib/gumiho-link`, owned by `gumiho-link:gumiho-link` with mode 0600.
5. Start the service and run both health checks.

Restoring rolls membership back to that snapshot. All clients refresh because every relay
restart creates a new protocol epoch. Live tactical data is never restored.

## Lost member or Founder key

If a member cannot authenticate, first list identities without exposing credentials:

    node /opt/gumiho-link/relay/src/operator.mjs list /etc/gumiho-link/relay.json

Offline recovery must be performed with the service stopped:

    systemctl stop gumiho-link
    node /opt/gumiho-link/relay/src/operator.mjs rotate-member \
      /etc/gumiho-link/relay.json <member-uuid> /root/private-recovery.json ROTATE_MEMBER_KEY
    systemctl start gumiho-link

The replacement key is written once to the requested mode-0600 file and is never printed.
Transfer it privately, confirm the member saved it, then securely delete the handoff file.
The old key stops working immediately.

## Updates and rollback

Before an update, create and verify a membership backup. The installer preserves the
existing config and bootstrap credential, saves an application/config tarball, and keeps
one adjacent `.codex-original` copy of an existing systemd unit. After updating, run the
automated tests, restart the service and verify local/public health. Roll back application
files without rolling back membership unless state recovery is specifically required.

Never expose port 8791, run Node as root, log bearer headers, or put private JSON files in
a modpack/source archive. Caddy and Proxmox firewall logs should not record request bodies
or authorization headers.
