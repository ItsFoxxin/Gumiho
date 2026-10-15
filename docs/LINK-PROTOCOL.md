# GUMIHO Link protocol 1 (development)

All routes return JSON. POST/PUT require application/json, maximum 16,384 bytes.
Credentials are bearer headers, never URL parameters, cookies or preset fields.
No request fields are accepted beyond each schema's whitelist.
All response content is untrusted display data to clients; escape/sanitize it.

The Node service always binds 127.0.0.1, rejects browser Origin headers and sends
Cache-Control: no-store. Development mode accepts loopback Host values only. Production
mode accepts an explicit hostname allowlist and validated client addresses from a trusted
loopback reverse proxy. The Proxmox LXC package uses Caddy for public HTTPS; Node port 8791
is never exposed. Browser-based clients remain unsupported.

## Credentials
- glb_ + 43 URL-safe characters: bootstrap, relay owner only, create-team route only.
- gli_ + 43 URL-safe characters: single-use invitation, join route only.
- gl1_ + 43 URL-safe characters: individual member token for that member's team.
Random payloads are 32 bytes. Membership state stores SHA-256 hashes, not raw tokens.
There is no client-supplied teamId on member operations. No public team directory.
Display names are invitation-holder claims, not verified Microsoft/Mojang identities.
Any shared gameplay data is also sender-reported, not authoritative anticheat evidence.

## Routes

| Method / route | Credential | Input / output |
| --- | --- | --- |
| GET /health | none | ok and protocol only |
| POST /v1/teams | bootstrap | teamName, displayName -> teamId, memberId, token |
| POST /v1/join | invitation | displayName -> teamId, memberId, token |
| GET /v1/session | member | epoch, team, self, nextSequence, liveTtlMs, own sharing |
| POST /v1/invites | member | empty object -> invite id, token, expiry |
| DELETE /v1/invites/:id | member | revoke own invite; founders/co-founders may revoke team invites |
| PUT /v1/members/:id/role | member | role; relay checks actor and target |
| DELETE /v1/members/:id | member | remove; token, live state, pings and issued invites invalidated |
| POST /v1/me/token | member | empty object -> replacement token; old token immediately invalid |
| PUT /v1/team/founder | Founder | memberId, confirmation=TRANSFER_FOUNDER -> leadership swap |
| PUT /v1/me/status | member | status schema below |
| POST /v1/me/disconnect | member | epoch, sequence; clears live state and own pings |
| GET /v1/snapshot | member | filtered members and pings for this team/context |
| POST /v1/pings | member | ping schema below -> id, expiry |
| DELETE /v1/me/pings | member | clear own pings |

No remote commands, module controls, inventory contents or enemy telemetry routes exist.

Founder transfer is explicit and server-enforced. The target must be the current Co-founder
when one exists; otherwise any non-Founder member is eligible. The former Founder becomes
Co-founder. Transfer clears invitations, pings and all live status so every client must
republish under its new exact role. It does not copy or reveal member credentials.

## Status schema
Required: epoch (from session), sequence (increasing safe integer), context, sharing.
Optional location/stats are replaced, not merged with the previous update.

    {
      "epoch": "current relay boot UUID",
      "sequence": 1,
      "context": {
        "serverKey": "64 lowercase hexadecimal SHA-256 characters",
        "dimension": "minecraft:overworld"
      },
      "sharing": {
        "locationRoles": ["COFOUNDER", "INNER_CIRCLE"],
        "statsRoles": ["COFOUNDER"]
      },
      "location": { "x": 120, "y": 64, "z": -300 },
      "stats": {
        "health": 12, "maxHealth": 20, "hunger": 18,
        "armorPercent": 72,
        "mainItem": "minecraft:bow", "offItem": "minecraft:shield"
      }
    }

context may be null in menus/off-world, but location/stats must then be absent or null.
armorPercent may be null if unknown/no equipment. No item stack contents or durability
inventory list is sent. A category with an empty audience is discarded even if sent.

The client should hash the same normalized server address and include custom ports.
Address aliases will produce different keys unless the client explicitly supports a
shared server identifier. Single-player sharing should remain disabled by default.
The key reduces raw-address disclosure; it is NOT a secret or a proof of server membership.

Publish about once per second, with monotonically increasing sequence. Read session first.
409 refresh_session means get the new relay epoch; 409 stale_sequence means an older update
was rejected. Never retry an old privacy update after a newer one.
No old-boot update may restore a previously shared location.

## Visibility and lifetime
All team members can see roster identities/roles and whether each member has a fresh session.
Sensitive status needs fresh sender AND viewer context, matching server key/dimension,
and the viewer's exact role in the sender's selected category audience.
There is no inheritance, role-ranking visibility shortcut or Founder bypass.
A viewer can see their own stored consented data; unshared fields are not retained.

Live state expires after 5 seconds without a publish. Snapshot includes validForMs,
serverTime, and no raw context key in other members' rows. Clients must also discard
expired data locally and clear caches on any network/auth/context error, disconnect,
share-off or role/membership update. Polling is not instantaneous erasure: a recipient
may retain a previously received snapshot until its short TTL and cannot be made to
forget data they already saw.

Persistent membership survives restart; live telemetry and pings do not.
Epoch changes on restart. Membership saves never contain live state.

## Ping schema

    {
      "epoch": "current relay boot UUID",
      "context": { "serverKey": "same key as live status", "dimension": "minecraft:overworld" },
      "position": { "x": 100, "y": 64, "z": 200 },
      "kind": "DANGER",
      "audienceRoles": ["COFOUNDER", "INNER_CIRCLE", "OFFICER", "MEMBER"]
    }

Kinds: LOCATION, DANGER, HELP, REGROUP. No arbitrary rich text, URLs or commands.
The sender must have a fresh matching published context. A ping deliberately shares
that point even when continuous location sharing is OFF; the client UI must warn clearly.
One ping per two seconds, maximum three active per sender and ten per team, TTL 30 seconds.
Context change, stale sender, explicit clear, disconnect or removal clears owned pings.

## Failure contract / operating limits
Errors are stable codes with no echoed tokens/request bodies.
400 invalid fields/numbers/context; 401 invalid credential; 403 forbidden role/origin;
404 target not in own team or unknown route; 409 state/sequence/capacity conflict;
413 oversized JSON; 415 wrong content type; 421 invalid Host; 429 rate cap;
503 membership persistence failure (mutation rolled back).
HTTP failure closes the request connection; clients should back off, not retry tight loops.

Current limits: 32 teams, 10 members/team, 10 live invites/team, 10-minute invites,
64 connections, 8 KiB headers, 5-second request/header timeout, 600 requests/minute/peer IP,
360 requests/minute/token hash, 2,048 rate buckets. Behind Caddy, the IP limit uses a
strictly validated first X-Forwarded-For address only because the Node socket is loopback-only.
