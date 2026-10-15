import { createHash, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';

export const ROLES = Object.freeze(['FOUNDER', 'COFOUNDER', 'INNER_CIRCLE', 'OFFICER', 'MEMBER']);
export const LIVE_MS = 5000, PING_MS = 30000, INVITE_MS = 600000;
export class RelayError extends Error {
  constructor(status, code) { super(code); this.status = status; this.code = code; }
}
const fail = (status, code) => { throw new RelayError(status, code); };
export const credential = (prefix = 'gl1') => prefix + '_' + randomBytes(32).toString('base64url');
const hash = value => createHash('sha256').update(value).digest('hex');
const idPattern = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/;
const resource = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const number = (value, min, max) => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < min || value > max)
    fail(400, 'invalid_number');
  return value;
};
const integer = (value, min, max) => {
  number(value, min, max);
  if (!Number.isSafeInteger(value)) fail(400, 'invalid_integer');
  return value;
};
const object = (value, allowed, required = allowed) => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) fail(400, 'invalid_object');
  if (Object.keys(value).some(key => !allowed.includes(key)) || required.some(key => !Object.hasOwn(value, key)))
    fail(400, 'invalid_fields');
  return value;
};
const label = value => {
  if (typeof value !== 'string' || value !== value.trim() || !/^[\p{L}\p{N} _-]{1,32}$/u.test(value))
    fail(400, 'invalid_name');
  return value;
};
const roles = value => {
  if (!Array.isArray(value) || value.length > ROLES.length || new Set(value).size !== value.length ||
      value.some(role => !ROLES.includes(role))) fail(400, 'invalid_roles');
  return [...value];
};
const uuid = value => {
  if (typeof value !== 'string' || !idPattern.test(value)) fail(400, 'invalid_id');
  return value;
};
const context = value => {
  if (value === null) return null;
  object(value, ['serverKey', 'dimension']);
  if (typeof value.serverKey !== 'string' || !/^[a-f0-9]{64}$/.test(value.serverKey) ||
      typeof value.dimension !== 'string' || value.dimension.length > 128 || !resource.test(value.dimension))
    fail(400, 'invalid_context');
  return { serverKey: value.serverKey, dimension: value.dimension };
};
const position = value => {
  object(value, ['x', 'y', 'z']);
  return { x: number(value.x, -30000000, 30000000), y: number(value.y, -10000, 10000),
    z: number(value.z, -30000000, 30000000) };
};
const stats = value => {
  object(value, ['health', 'maxHealth', 'hunger', 'armorPercent', 'mainItem', 'offItem']);
  const maxHealth = number(value.maxHealth, 1, 2048);
  for (const key of ['mainItem', 'offItem'])
    if (typeof value[key] !== 'string' || value[key].length > 128 || !resource.test(value[key]))
      fail(400, 'invalid_item');
  return { health: number(value.health, 0, maxHealth), maxHealth, hunger: integer(value.hunger, 0, 20),
    armorPercent: value.armorPercent === null ? null : number(value.armorPercent, 0, 100),
    mainItem: value.mainItem, offItem: value.offItem };
};
const sameContext = (a, b) => !!a && !!b && a.serverKey === b.serverKey && a.dimension === b.dimension;
const tokenHash = (token, prefix = 'gl1') => {
  if (typeof token !== 'string' || !new RegExp('^' + prefix + '_[A-Za-z0-9_-]{43}$').test(token))
    fail(401, 'unauthorized');
  return hash(token);
};

/** Bounded private-team state. Telemetry never enters persisted membership state. */
export class RelayCore {
  #teams = new Map();
  #bootstrapHash;
  #clock;
  #persist;
  constructor({ bootstrapToken, clock = Date.now, persist = () => {}, state = null } = {}) {
    this.#bootstrapHash = tokenHash(bootstrapToken, 'glb');
    this.#clock = clock;
    this.#persist = persist;
    this.epoch = randomUUID();
    if (state !== null) this.#restore(state);
  }
  #auth(token) {
    const digest = tokenHash(token);
    for (const team of this.#teams.values())
      for (const member of team.members.values())
        if (member.tokenHash === digest) return { team, member };
    fail(401, 'unauthorized');
  }
  #target(team, id) {
    const target = team.members.get(uuid(id));
    if (!target) fail(404, 'not_found');
    return target;
  }
  #transaction(action) {
    const before = structuredClone(this.#teams);
    try {
      const result = action();
      this.#persist(this.exportState());
      return result;
    } catch (error) {
      this.#teams = before;
      if (error instanceof RelayError) throw error;
      throw new RelayError(503, 'storage_unavailable');
    }
  }
  #fresh(member) {
    return member.live && this.#clock() - member.live.updatedAt < LIVE_MS;
  }
  #clearPings(team, memberId) {
    for (const [id, ping] of team.pings) if (ping.senderId === memberId) team.pings.delete(id);
  }
  #clearInvites(team, issuerId) {
    for (const [id, invite] of team.invites) if (invite.issuerId === issuerId) team.invites.delete(id);
  }
  sweep() {
    const now = this.#clock();
    for (const team of this.#teams.values()) {
      for (const [id, invite] of team.invites) if (invite.expiresAt <= now) team.invites.delete(id);
      for (const member of team.members.values()) {
        if (member.live && !this.#fresh(member)) {
          member.live = null;
          this.#clearPings(team, member.id);
        }
      }
      for (const [id, ping] of team.pings) if (ping.expiresAt <= now) team.pings.delete(id);
    }
  }
  #newMember(name, role) {
    const token = credential();
    return { token, member: { id: randomUUID(), name: label(name), role, tokenHash: hash(token),
      sequence: 0, live: null, lastPingAt: -Infinity } };
  }
  #viewMember(member) { return { id: member.id, name: member.name, role: member.role }; }
  createTeam(token, input) {
    const supplied = tokenHash(token, 'glb');
    if (!timingSafeEqual(Buffer.from(supplied, 'hex'), Buffer.from(this.#bootstrapHash, 'hex')))
      fail(401, 'unauthorized');
    object(input, ['teamName', 'displayName']);
    label(input.teamName); label(input.displayName);
    if (this.#teams.size >= 32) fail(409, 'relay_full');
    return this.#transaction(() => {
      const { member, token: memberToken } = this.#newMember(input.displayName, 'FOUNDER');
      const team = { id: randomUUID(), name: input.teamName, members: new Map([[member.id, member]]),
        invites: new Map(), pings: new Map() };
      this.#teams.set(team.id, team);
      return { teamId: team.id, memberId: member.id, token: memberToken };
    });
  }
  session(token) {
    this.sweep();
    const { team, member } = this.#auth(token);
    return { protocol: 1, epoch: this.epoch, team: { id: team.id, name: team.name },
      self: this.#viewMember(member), nextSequence: member.sequence + 1, liveTtlMs: LIVE_MS,
      sharing: member.live?.sharing ?? { locationRoles: [], statsRoles: [] } };
  }
  invite(token) {
    this.sweep();
    const { team, member } = this.#auth(token);
    if (!['FOUNDER', 'COFOUNDER', 'OFFICER'].includes(member.role)) fail(403, 'forbidden');
    if (team.members.size >= 10) fail(409, 'team_full');
    if (team.invites.size >= 10) fail(429, 'too_many_invites');
    return this.#transaction(() => {
      const inviteToken = credential('gli'), id = randomUUID(), expiresAt = this.#clock() + INVITE_MS;
      team.invites.set(id, { id, tokenHash: hash(inviteToken), issuerId: member.id, expiresAt });
      return { id, token: inviteToken, expiresAt, role: 'MEMBER' };
    });
  }
  revokeInvite(token, id) {
    const { team, member } = this.#auth(token);
    const invite = team.invites.get(uuid(id));
    if (!invite) fail(404, 'not_found');
    if (invite.issuerId !== member.id && !['FOUNDER', 'COFOUNDER'].includes(member.role)) fail(403, 'forbidden');
    return this.#transaction(() => { team.invites.delete(id); return { ok: true }; });
  }
  join(inviteToken, input) {
    this.sweep();
    const digest = tokenHash(inviteToken, 'gli');
    object(input, ['displayName']); label(input.displayName);
    for (const team of this.#teams.values()) {
      for (const invite of team.invites.values()) {
        if (invite.tokenHash !== digest) continue;
        if (team.members.size >= 10) fail(409, 'team_full');
        if ([...team.members.values()].some(m => m.name.toLowerCase() === input.displayName.toLowerCase()))
          fail(409, 'name_in_use');
        return this.#transaction(() => {
          const { member, token } = this.#newMember(input.displayName, 'MEMBER');
          team.members.set(member.id, member); team.invites.delete(invite.id);
          return { teamId: team.id, memberId: member.id, token };
        });
      }
    }
    fail(401, 'unauthorized');
  }
  setRole(token, id, input) {
    const { team, member } = this.#auth(token), target = this.#target(team, id);
    object(input, ['role']);
    const role = input.role;
    if (!ROLES.includes(role) || role === 'FOUNDER') fail(400, 'invalid_role');
    if (target.id === member.id || target.role === 'FOUNDER') fail(403, 'forbidden');
    if (member.role !== 'FOUNDER' && !(member.role === 'COFOUNDER' &&
        target.role !== 'COFOUNDER' && role !== 'COFOUNDER')) fail(403, 'forbidden');
    if (role === 'COFOUNDER' && [...team.members.values()].some(m => m.id !== id && m.role === 'COFOUNDER'))
      fail(409, 'cofounder_exists');
    return this.#transaction(() => {
      target.role = role;
      // Old invitations must not survive an issuer's permission change.
      this.#clearInvites(team, id);
      return this.#viewMember(target);
    });
  }
  transferFounder(token, input) {
    const { team, member } = this.#auth(token);
    object(input, ['memberId', 'confirmation']);
    if (member.role !== 'FOUNDER' || input.confirmation !== 'TRANSFER_FOUNDER') fail(403, 'forbidden');
    const target = this.#target(team, input.memberId);
    if (target.id === member.id || target.role === 'FOUNDER') fail(403, 'forbidden');
    const currentCo = [...team.members.values()].find(candidate => candidate.role === 'COFOUNDER');
    if (currentCo && currentCo.id !== target.id) fail(409, 'transfer_to_cofounder_first');
    return this.#transaction(() => {
      member.role = 'COFOUNDER';
      target.role = 'FOUNDER';
      // Leadership changes invalidate every unused invitation and live tactical view.
      // Clients republish under their new exact roles after refreshing their session.
      team.invites.clear(); team.pings.clear();
      for (const candidate of team.members.values()) candidate.live = null;
      return { founder: this.#viewMember(target), previousFounder: this.#viewMember(member) };
    });
  }
  remove(token, id) {
    const { team, member } = this.#auth(token), target = this.#target(team, id);
    if (target.role === 'FOUNDER' || member.id === id) fail(403, 'forbidden');
    if (member.role !== 'FOUNDER' &&
        !(member.role === 'COFOUNDER' && target.role !== 'COFOUNDER') &&
        !(member.role === 'OFFICER' && target.role === 'MEMBER')) fail(403, 'forbidden');
    return this.#transaction(() => {
      team.members.delete(id); this.#clearInvites(team, id); this.#clearPings(team, id);
      return { ok: true };
    });
  }
  rotateToken(token) {
    const { member } = this.#auth(token);
    return this.#transaction(() => {
      const next = credential(); member.tokenHash = hash(next);
      return { token: next };
    });
  }
  #sequence(member, input) {
    if (input.epoch !== this.epoch) fail(409, 'refresh_session');
    integer(input.sequence, 1, Number.MAX_SAFE_INTEGER - 1);
    if (input.sequence <= member.sequence) fail(409, 'stale_sequence');
  }
  publish(token, input) {
    this.sweep();
    const { team, member } = this.#auth(token);
    object(input, ['epoch', 'sequence', 'context', 'sharing', 'location', 'stats'],
      ['epoch', 'sequence', 'context', 'sharing']);
    this.#sequence(member, input);
    const scope = context(input.context);
    object(input.sharing, ['locationRoles', 'statsRoles']);
    const sharing = { locationRoles: roles(input.sharing.locationRoles), statsRoles: roles(input.sharing.statsRoles) };
    const location = input.location == null ? null : position(input.location);
    const values = input.stats == null ? null : stats(input.stats);
    if (!scope && (location || values)) fail(400, 'context_required');
    if (!sameContext(member.live?.context, scope)) this.#clearPings(team, member.id);
    member.sequence = input.sequence;
    member.live = { context: scope, sharing,
      // Never retain fields that the sender has not elected to share.
      location: sharing.locationRoles.length ? location : null,
      stats: sharing.statsRoles.length ? values : null, updatedAt: this.#clock() };
    return { sequence: member.sequence, expiresAt: member.live.updatedAt + LIVE_MS };
  }
  disconnect(token, input) {
    const { team, member } = this.#auth(token);
    object(input, ['epoch', 'sequence']); this.#sequence(member, input);
    member.sequence = input.sequence; member.live = null; this.#clearPings(team, member.id);
    return { ok: true };
  }
  ping(token, input) {
    this.sweep();
    const { team, member } = this.#auth(token);
    object(input, ['epoch', 'context', 'position', 'kind', 'audienceRoles']);
    if (input.epoch !== this.epoch) fail(409, 'refresh_session');
    const scope = context(input.context), point = position(input.position), audienceRoles = roles(input.audienceRoles);
    if (!['LOCATION', 'DANGER', 'HELP', 'REGROUP'].includes(input.kind) || !audienceRoles.length)
      fail(400, 'invalid_ping');
    if (!this.#fresh(member) || !sameContext(member.live.context, scope)) fail(409, 'publish_context_first');
    if (this.#clock() - member.lastPingAt < 2000) fail(429, 'ping_rate_limited');
    if (team.pings.size >= 10 || [...team.pings.values()].filter(p => p.senderId === member.id).length >= 3)
      fail(429, 'too_many_pings');
    const ping = { id: randomUUID(), senderId: member.id, context: scope, position: point, kind: input.kind,
      audienceRoles, expiresAt: this.#clock() + PING_MS };
    team.pings.set(ping.id, ping); member.lastPingAt = this.#clock();
    return { id: ping.id, expiresAt: ping.expiresAt };
  }
  clearPings(token) {
    const { team, member } = this.#auth(token);
    this.#clearPings(team, member.id); return { ok: true };
  }
  snapshot(token) {
    this.sweep();
    const { team, member: viewer } = this.#auth(token), now = this.#clock();
    const viewerContext = this.#fresh(viewer) ? viewer.live.context : null;
    const members = [...team.members.values()].map(member => {
      const live = this.#fresh(member) ? member.live : null;
      const allowed = live && sameContext(viewerContext, live.context);
      const own = member.id === viewer.id;
      return { ...this.#viewMember(member), online: !!live,
        location: allowed && (own || live.sharing.locationRoles.includes(viewer.role)) ? live.location : null,
        stats: allowed && (own || live.sharing.statsRoles.includes(viewer.role)) ? live.stats : null,
        validForMs: live ? Math.max(0, LIVE_MS - (now - live.updatedAt)) : 0 };
    });
    const pings = [...team.pings.values()].filter(p => sameContext(viewerContext, p.context) &&
      (p.senderId === viewer.id || p.audienceRoles.includes(viewer.role))).map(p => ({
        id: p.id, senderId: p.senderId, position: p.position, kind: p.kind, expiresAt: p.expiresAt
      }));
    return { protocol: 1, epoch: this.epoch, serverTime: now, members, pings };
  }
  exportState() {
    return { version: 1, teams: [...this.#teams.values()].map(team => ({
      id: team.id, name: team.name,
      members: [...team.members.values()].map(({ id, name, role, tokenHash }) => ({ id, name, role, tokenHash })),
      invites: [...team.invites.values()].map(invite => ({ ...invite }))
    })) };
  }
  #restore(state) {
    object(state, ['version', 'teams']);
    if (state.version !== 1 || !Array.isArray(state.teams) || state.teams.length > 32) fail(400, 'invalid_store');
    const seenIds = new Set(), seenHashes = new Set();
    const remember = (set, value) => { if (set.has(value)) fail(400, 'invalid_store'); set.add(value); return value; };
    const digest = value => {
      if (typeof value !== 'string' || !/^[a-f0-9]{64}$/.test(value)) fail(400, 'invalid_store');
      return remember(seenHashes, value);
    };
    for (const item of state.teams) {
      object(item, ['id', 'name', 'members', 'invites']);
      if (!Array.isArray(item.members) || item.members.length < 1 || item.members.length > 10 ||
          !Array.isArray(item.invites) || item.invites.length > 10) fail(400, 'invalid_store');
      const team = { id: remember(seenIds, uuid(item.id)), name: label(item.name),
        members: new Map(), invites: new Map(), pings: new Map() };
      for (const data of item.members) {
        object(data, ['id', 'name', 'role', 'tokenHash']);
        if (!ROLES.includes(data.role)) fail(400, 'invalid_store');
        const member = { id: remember(seenIds, uuid(data.id)), name: label(data.name),
          role: data.role, tokenHash: digest(data.tokenHash), sequence: 0, live: null, lastPingAt: -Infinity };
        if ([...team.members.values()].some(m => m.name.toLowerCase() === member.name.toLowerCase()))
          fail(400, 'invalid_store');
        team.members.set(member.id, member);
      }
      if ([...team.members.values()].filter(m => m.role === 'FOUNDER').length !== 1 ||
          [...team.members.values()].filter(m => m.role === 'COFOUNDER').length > 1) fail(400, 'invalid_store');
      for (const data of item.invites) {
        object(data, ['id', 'tokenHash', 'issuerId', 'expiresAt']);
        const issuerId = uuid(data.issuerId), issuer = team.members.get(issuerId);
        if (!issuer || !['FOUNDER', 'COFOUNDER', 'OFFICER'].includes(issuer.role)) fail(400, 'invalid_store');
        const invite = { id: remember(seenIds, uuid(data.id)), tokenHash: digest(data.tokenHash),
          issuerId, expiresAt: integer(data.expiresAt, 0, Number.MAX_SAFE_INTEGER) };
        if (invite.expiresAt > this.#clock()) team.invites.set(invite.id, invite);
      }
      this.#teams.set(team.id, team);
    }
  }
}
