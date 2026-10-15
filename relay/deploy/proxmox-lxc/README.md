# Proxmox LXC target

Use a dedicated **unprivileged** Debian LXC. The relay does not need nesting, Docker,
privileged mode, GPU access or a Minecraft server installation.

Suggested starting allocation:

- 1 vCPU
- 1 GiB RAM and 512 MiB swap
- 8 GiB disk
- DHCP reservation or static LAN address
- Proxmox firewall enabled

On the Proxmox host, select the current Debian standard template. A typical creation
shape is:

    pct create <VMID> <storage>:vztmpl/<debian-template> \
      --hostname gumiho-link --unprivileged 1 --cores 1 --memory 1024 --swap 512 \
      --rootfs <storage>:8 --net0 name=eth0,bridge=vmbr0,ip=dhcp

Do not copy that command blindly: substitute the VMID, storage, bridge and exact template
present on your Proxmox host. Start the LXC and reserve its address in DHCP.

Inside the container:

1. Install Node.js 24 or newer and Caddy from their maintained package sources.
2. Copy/extract this relay bundle to a temporary directory.
3. Run `sh deploy/proxmox-lxc/install.sh` as root. It installs files but deliberately does not start the service.
4. Replace `link.example.com` in `/etc/gumiho-link/relay.json` with the exact public DNS name.
5. Copy `Caddyfile.example` to `/etc/caddy/Caddyfile`, replace the same hostname and validate Caddy.
6. Point public DNS to your WAN address and forward TCP 80/443 to this LXC, or place the
   LXC behind your existing HTTPS ingress. Never forward port 8791.
7. Start Caddy, then `systemctl enable --now gumiho-link`.
8. Run the local and public health checks in `OPERATIONS.md` before distributing invitations.

The Node process always binds `127.0.0.1:8791`. Caddy terminates TLS and forwards the
original exact Host plus the client address. The relay rejects other Host values and
browser Origin requests. Only HTTPS port 443 should be shared with members.

This template assumes Caddy receives Internet traffic directly. If another CDN or reverse
proxy is added in front later, configure Caddy's trusted proxy handling before relying on
forwarded client addresses; do not add arbitrary forwarded-header overrides to this template.
