#!/bin/sh
set -eu

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root inside the dedicated LXC." >&2
  exit 1
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELAY_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
NODE_MAJOR=$(node -p "Number(process.versions.node.split('.')[0])" 2>/dev/null || echo 0)
if [ "$NODE_MAJOR" -lt 24 ]; then
  echo "Node.js 24 or newer is required before installation." >&2
  exit 1
fi

if ! id gumiho-link >/dev/null 2>&1; then
  useradd --system --home-dir /var/lib/gumiho-link --shell /usr/sbin/nologin gumiho-link
fi
install -d -m 0755 -o root -g root /opt/gumiho-link/relay /opt/gumiho-link/relay/src
install -d -m 0750 -o root -g gumiho-link /etc/gumiho-link
install -d -m 0700 -o gumiho-link -g gumiho-link /var/lib/gumiho-link
install -d -m 0700 -o root -g root /var/backups/gumiho-link

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
if [ -f /opt/gumiho-link/relay/package.json ]; then
  tar --numeric-owner -czf "/var/backups/gumiho-link/pre-install-$STAMP.tgz" \
    /opt/gumiho-link/relay /etc/gumiho-link 2>/dev/null || true
fi

install -m 0644 -o root -g root "$RELAY_ROOT/package.json" /opt/gumiho-link/relay/package.json
for source in "$RELAY_ROOT"/src/*.mjs; do
  install -m 0644 -o root -g root "$source" "/opt/gumiho-link/relay/src/$(basename "$source")"
done
install -m 0644 -o root -g root "$RELAY_ROOT/OPERATIONS.md" /opt/gumiho-link/relay/OPERATIONS.md

if [ ! -e /etc/gumiho-link/relay.json ]; then
  install -m 0640 -o root -g gumiho-link "$SCRIPT_DIR/relay.example.json" /etc/gumiho-link/relay.json
  echo "Created /etc/gumiho-link/relay.json; replace link.example.com before starting."
else
  echo "Preserved existing /etc/gumiho-link/relay.json."
fi

SERVICE=/etc/systemd/system/gumiho-link.service
if [ -e "$SERVICE" ] && [ ! -e "$SERVICE.codex-original" ]; then
  cp -a "$SERVICE" "$SERVICE.codex-original"
fi
install -m 0644 -o root -g root "$SCRIPT_DIR/gumiho-link.service" "$SERVICE"

if [ ! -e /var/lib/gumiho-link/private-bootstrap.json ]; then
  runuser -u gumiho-link -- /usr/bin/node /opt/gumiho-link/relay/src/main.mjs \
    --init /var/lib/gumiho-link/private-bootstrap.json
else
  echo "Preserved existing private bootstrap credential."
fi
chmod 0600 /var/lib/gumiho-link/private-*.json 2>/dev/null || true
chown gumiho-link:gumiho-link /var/lib/gumiho-link/private-*.json 2>/dev/null || true
systemctl daemon-reload

echo "Installed but NOT started. Configure the exact public hostname and Caddy first."
echo "Then run: systemctl enable --now gumiho-link"
