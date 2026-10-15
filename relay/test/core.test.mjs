import test from 'node:test';
import assert from 'node:assert/strict';
import { RelayCore, RelayError, credential, ROLES, LIVE_MS, PING_MS, INVITE_MS } from '../src/core.mjs';

const realm = { serverKey: '1'.repeat(64), dimension: 'minecraft:overworld' };
const point = { x: 120, y: 64, z: -300 };
const values = { health: 12, maxHealth: 20, hunger: 18, armorPercent: 72,
  mainItem: 'minecraft:bow', offItem: 'minecraft:shield' };
const denied = (action, status) => assert.throws(action, e => e instanceof RelayError && e.status === status);
function fixture(options = {}) {
  let now = 1000000;
  const bootstrapToken = credential('glb');
  const core = new RelayCore({ bootstrapToken, clock: () => now, ...options });
  const founder = core.createTeam(bootstrapToken, { teamName: '구미호', displayName: 'Founder' });
  const add = name => core.join(core.invite(founder.token).token, { displayName: name });
  const publish = (who, patch = {}) => core.publish(who.token, {
    epoch: core.epoch, sequence: core.session(who.token).nextSequence, context: realm,
    sharing: { locationRoles: [], statsRoles: [] }, ...patch });
  return { core, founder, add, publish, bootstrapToken, advance: delta => { now += delta; } };
}
const from = (core, viewer, member) => core.snapshot(viewer.token).members.find(m => m.id === member.memberId);

test('private by default; no founder or cofounder location backdoor', () => {
  const f = fixture(), bob = f.add('Bob');
  f.publish(f.founder); f.publish(bob, { location: point, stats: values });
  assert.equal(from(f.core, f.founder, bob).location, null);
  assert.equal(from(f.core, f.founder, bob).stats, null);
  f.core.setRole(f.founder.token, bob.memberId, { role: 'COFOUNDER' });
  assert.equal(from(f.core, bob, f.founder).location, null);
  const stored = JSON.stringify(f.core.exportState());
  for (const privateValue of [bob.token, f.founder.token, 'serverKey', 'location', 'stats', 'hunger'])
    assert.ok(!stored.includes(privateValue));
});

test('all 25 sender-selected role / viewer role pairs obey exact role visibility', () => {
  const f = fixture();
  const viewers = [f.founder, ...['Co', 'Inner', 'Officer', 'Member'].map(f.add)];
  for (let i = 1; i < viewers.length; i++)
    f.core.setRole(f.founder.token, viewers[i].memberId, { role: ROLES[i] });
  const sender = f.add('Sender');
  for (const viewer of viewers) f.publish(viewer);
  for (const allowedRole of ROLES) {
    f.publish(sender, { sharing: { locationRoles: [allowedRole], statsRoles: [allowedRole] },
      location: point, stats: values });
    for (let i = 0; i < viewers.length; i++) {
      const visible = from(f.core, viewers[i], sender);
      assert.deepEqual(visible.location, ROLES[i] === allowedRole ? point : null);
      assert.deepEqual(visible.stats, ROLES[i] === allowedRole ? values : null);
    }
  }
});

test('stats and location consent are independent and share-off removes old data', () => {
  const f = fixture(), bob = f.add('Bob'); f.publish(f.founder);
  f.publish(bob, { location: point, stats: values, sharing: { locationRoles: [], statsRoles: ['FOUNDER'] } });
  assert.equal(from(f.core, f.founder, bob).location, null);
  assert.deepEqual(from(f.core, f.founder, bob).stats, values);
  f.publish(bob, { sharing: { locationRoles: ['FOUNDER'], statsRoles: [] }, location: point });
  assert.deepEqual(from(f.core, f.founder, bob).location, point);
  assert.equal(from(f.core, f.founder, bob).stats, null);
  f.publish(bob);
  assert.equal(from(f.core, f.founder, bob).location, null);
});

test('Founder transfer is explicit, preserves one Founder and clears volatile state', () => {
  const f = fixture(), co = f.add('Co'), member = f.add('Member');
  f.core.setRole(f.founder.token, co.memberId, { role: 'COFOUNDER' });
  f.publish(f.founder); f.publish(co); f.publish(member);
  const invite = f.core.invite(f.founder.token);
  denied(() => f.core.transferFounder(co.token,
    { memberId: co.memberId, confirmation: 'TRANSFER_FOUNDER' }), 403);
  denied(() => f.core.transferFounder(f.founder.token,
    { memberId: member.memberId, confirmation: 'TRANSFER_FOUNDER' }), 409);
  denied(() => f.core.transferFounder(f.founder.token,
    { memberId: co.memberId, confirmation: 'wrong' }), 403);
  const result = f.core.transferFounder(f.founder.token,
    { memberId: co.memberId, confirmation: 'TRANSFER_FOUNDER' });
  assert.equal(result.founder.role, 'FOUNDER');
  assert.equal(result.previousFounder.role, 'COFOUNDER');
  assert.equal(f.core.session(co.token).self.role, 'FOUNDER');
  assert.equal(f.core.session(f.founder.token).self.role, 'COFOUNDER');
  assert.equal(f.core.snapshot(co.token).members.every(row => !row.online), true);
  denied(() => f.core.join(invite.token, { displayName: 'Late' }), 401);
  denied(() => f.core.transferFounder(f.founder.token,
    { memberId: member.memberId, confirmation: 'TRANSFER_FOUNDER' }), 403);
});

test('Founder may transfer directly when no Co-founder exists', () => {
  const f = fixture(), member = f.add('Member');
  f.core.transferFounder(f.founder.token,
    { memberId: member.memberId, confirmation: 'TRANSFER_FOUNDER' });
  assert.equal(f.core.session(member.token).self.role, 'FOUNDER');
  assert.equal(f.core.session(f.founder.token).self.role, 'COFOUNDER');
});

test('role downgrade and promotion re-evaluate visibility on the next snapshot', () => {
  const f = fixture(), sender = f.add('Sender'), viewer = f.add('Viewer');
  f.publish(sender, { location: point, sharing: { locationRoles: ['INNER_CIRCLE'], statsRoles: [] } });
  f.publish(viewer);
  assert.equal(from(f.core, viewer, sender).location, null);
  f.core.setRole(f.founder.token, viewer.memberId, { role: 'INNER_CIRCLE' });
  assert.deepEqual(from(f.core, viewer, sender).location, point);
  f.core.setRole(f.founder.token, viewer.memberId, { role: 'MEMBER' });
  assert.equal(from(f.core, viewer, sender).location, null);
});

test('independent teams cannot enumerate, promote or remove each other', () => {
  const f = fixture();
  const other = f.core.createTeam(f.bootstrapToken, { teamName: 'Other', displayName: 'OtherOwner' });
  assert.equal(f.core.snapshot(f.founder.token).members.length, 1);
  denied(() => f.core.setRole(other.token, f.founder.memberId, { role: 'MEMBER' }), 404);
  denied(() => f.core.remove(f.founder.token, other.memberId), 404);
  denied(() => f.core.revokeInvite(other.token, f.core.invite(f.founder.token).id), 404);
});

test('server and dimension boundaries hide status and pings', () => {
  const f = fixture(), bob = f.add('Bob');
  f.publish(f.founder, { location: point, stats: values, sharing: { locationRoles: ['MEMBER'], statsRoles: ['MEMBER'] } });
  f.core.ping(f.founder.token, { epoch: f.core.epoch, context: realm, position: point,
    kind: 'DANGER', audienceRoles: ['MEMBER'] });
  for (const context of [null, { ...realm, dimension: 'minecraft:the_nether' }, { ...realm, serverKey: '2'.repeat(64) }]) {
    f.publish(bob, { context });
    assert.equal(from(f.core, bob, f.founder).location, null);
    assert.equal(from(f.core, bob, f.founder).stats, null);
    assert.equal(f.core.snapshot(bob.token).pings.length, 0);
  }
  f.publish(bob);
  assert.deepEqual(from(f.core, bob, f.founder).location, point);
  assert.equal(f.core.snapshot(bob.token).pings.length, 1);
});

test('disconnect clears live data and pings, and old sequence cannot resurrect it', () => {
  const f = fixture(), bob = f.add('Bob'); f.publish(bob);
  const published = f.publish(f.founder, { location: point, sharing: { locationRoles: ['MEMBER'], statsRoles: [] } });
  f.core.ping(f.founder.token, { epoch: f.core.epoch, context: realm, position: point, kind: 'HELP', audienceRoles: ['MEMBER'] });
  f.core.disconnect(f.founder.token, { epoch: f.core.epoch, sequence: published.sequence + 1 });
  assert.equal(from(f.core, bob, f.founder).online, false);
  assert.equal(f.core.snapshot(bob.token).pings.length, 0);
  denied(() => f.publish(f.founder, { sequence: published.sequence, location: point }), 409);
  denied(() => f.publish(f.founder, { sequence: published.sequence + 1 }), 409);
});

test('stale live data is evicted at the TTL and a stale viewer sees no live details', () => {
  const f = fixture(), bob = f.add('Bob'); f.publish(bob);
  f.publish(f.founder, { location: point, sharing: { locationRoles: ['MEMBER'], statsRoles: [] } });
  f.advance(LIVE_MS - 1);
  assert.deepEqual(from(f.core, bob, f.founder).location, point);
  assert.equal(from(f.core, bob, f.founder).validForMs, 1);
  f.advance(1);
  assert.equal(from(f.core, bob, f.founder).location, null);
  assert.equal(from(f.core, bob, f.founder).online, false);
  f.publish(f.founder, { location: point, sharing: { locationRoles: ['MEMBER'], statsRoles: [] } });
  assert.equal(from(f.core, bob, f.founder).location, null);
});

test('single-use invitations expire, can be revoked, and cannot grant a role on join', () => {
  const f = fixture();
  const invite = f.core.invite(f.founder.token);
  denied(() => f.core.join(invite.token, { displayName: 'Bob', role: 'FOUNDER' }), 400);
  const bob = f.core.join(invite.token, { displayName: 'Bob' });
  assert.equal(f.core.session(bob.token).self.role, 'MEMBER');
  denied(() => f.core.join(invite.token, { displayName: 'Again' }), 401);
  const stale = f.core.invite(f.founder.token);
  f.advance(INVITE_MS);
  denied(() => f.core.join(stale.token, { displayName: 'Late' }), 401);
  const revoked = f.core.invite(f.founder.token);
  f.core.revokeInvite(f.founder.token, revoked.id);
  denied(() => f.core.join(revoked.token, { displayName: 'Revoked' }), 401);
});

test('team capacity is ten, even when invitations were issued before the last join', () => {
  const f = fixture();
  const invites = Array.from({ length: 10 }, () => f.core.invite(f.founder.token));
  let last;
  for (let i = 0; i < 9; i++) last = f.core.join(invites[i].token, { displayName: 'Member' + i });
  denied(() => f.core.join(invites[9].token, { displayName: 'Eleventh' }), 409);
  denied(() => f.core.invite(f.founder.token), 409);
  assert.equal(f.core.snapshot(f.founder.token).members.length, 10);
  f.core.remove(f.founder.token, last.memberId);
  f.core.join(invites[9].token, { displayName: 'Replacement' });
  assert.equal(f.core.snapshot(f.founder.token).members.length, 10);
});

test('least-privilege role management and invitation invalidation', () => {
  const f = fixture(), co = f.add('Co'), inner = f.add('Inner'), officer = f.add('Officer'), member = f.add('Member');
  f.core.setRole(f.founder.token, co.memberId, { role: 'COFOUNDER' });
  f.core.setRole(co.token, inner.memberId, { role: 'INNER_CIRCLE' });
  f.core.setRole(co.token, officer.memberId, { role: 'OFFICER' });
  denied(() => f.core.invite(inner.token), 403);
  denied(() => f.core.invite(member.token), 403);
  denied(() => f.core.setRole(officer.token, member.memberId, { role: 'OFFICER' }), 403);
  denied(() => f.core.setRole(co.token, member.memberId, { role: 'COFOUNDER' }), 403);
  denied(() => f.core.setRole(f.founder.token, member.memberId, { role: 'COFOUNDER' }), 409);
  denied(() => f.core.setRole(co.token, co.memberId, { role: 'MEMBER' }), 403);
  denied(() => f.core.remove(co.token, f.founder.memberId), 403);
  denied(() => f.core.remove(officer.token, inner.memberId), 403);
  const invitation = f.core.invite(officer.token);
  f.core.setRole(f.founder.token, officer.memberId, { role: 'MEMBER' });
  denied(() => f.core.join(invitation.token, { displayName: 'Late' }), 401);
});

test('removed member token is invalid and their pings and invitations disappear', () => {
  const f = fixture(), bob = f.add('Bob');
  f.core.setRole(f.founder.token, bob.memberId, { role: 'OFFICER' });
  const invite = f.core.invite(bob.token);
  f.publish(f.founder); f.publish(bob);
  f.core.ping(bob.token, { epoch: f.core.epoch, context: realm, position: point, kind: 'LOCATION', audienceRoles: ['FOUNDER'] });
  f.core.remove(f.founder.token, bob.memberId);
  denied(() => f.core.session(bob.token), 401);
  denied(() => f.core.join(invite.token, { displayName: 'Late' }), 401);
  assert.equal(f.core.snapshot(f.founder.token).pings.length, 0);
});

test('manual pings work while continuous location sharing is off, with role filtering', () => {
  const f = fixture(), co = f.add('Co'), member = f.add('Member');
  f.core.setRole(f.founder.token, co.memberId, { role: 'COFOUNDER' });
  for (const who of [f.founder, co, member]) f.publish(who);
  f.core.ping(f.founder.token, { epoch: f.core.epoch, context: realm, position: point,
    kind: 'REGROUP', audienceRoles: ['COFOUNDER'] });
  assert.equal(from(f.core, co, f.founder).location, null);
  assert.deepEqual(f.core.snapshot(co.token).pings[0].position, point);
  assert.equal(f.core.snapshot(member.token).pings.length, 0);
  denied(() => f.core.ping(f.founder.token, { epoch: f.core.epoch, context: realm, position: point,
    kind: 'DANGER', audienceRoles: ['MEMBER'] }), 429);
  f.core.clearPings(f.founder.token);
  assert.equal(f.core.snapshot(co.token).pings.length, 0);
});

test('ping expiry, per-sender cap and context changes are enforced', () => {
  const f = fixture(), bob = f.add('Bob');
  const send = () => f.core.ping(f.founder.token, { epoch: f.core.epoch, context: realm, position: point,
    kind: 'HELP', audienceRoles: ['MEMBER'] });
  f.publish(f.founder); f.publish(bob);
  for (let i = 0; i < 3; i++) { send(); f.advance(2000); f.publish(f.founder); f.publish(bob); }
  denied(send, 429);
  f.publish(f.founder, { context: { ...realm, dimension: 'minecraft:the_end' } });
  assert.equal(f.core.snapshot(bob.token).pings.length, 0);
  f.publish(f.founder); f.advance(2000); send();
  for (let i = 0; i < PING_MS / 1000; i++) { f.advance(1000); f.publish(f.founder); f.publish(bob); }
  assert.equal(f.core.snapshot(bob.token).pings.length, 0);
});

test('token rotation invalidates the old token and membership persists without telemetry', () => {
  const f = fixture(), bob = f.add('Bob');
  f.publish(bob, { location: point, stats: values, sharing: { locationRoles: ['FOUNDER'], statsRoles: ['FOUNDER'] } });
  const next = f.core.rotateToken(bob.token);
  denied(() => f.core.session(bob.token), 401);
  const restored = new RelayCore({ bootstrapToken: f.bootstrapToken, state: f.core.exportState() });
  assert.equal(restored.session(next.token).self.id, bob.memberId);
  assert.equal(restored.snapshot(next.token).members.find(m => m.id === bob.memberId).online, false);
  denied(() => restored.publish(next.token, { epoch: f.core.epoch, sequence: 1, context: realm,
    sharing: { locationRoles: [], statsRoles: [] } }), 409);
});

test('failed membership persistence rolls back invitation consumption and membership mutation', () => {
  let failSave = false;
  const f = fixture({ persist: () => { if (failSave) throw new Error('disk full'); } });
  const invite = f.core.invite(f.founder.token);
  failSave = true;
  denied(() => f.core.join(invite.token, { displayName: 'Bob' }), 503);
  assert.equal(f.core.snapshot(f.founder.token).members.length, 1);
  failSave = false;
  const bob = f.core.join(invite.token, { displayName: 'Bob' });
  failSave = true;
  denied(() => f.core.remove(f.founder.token, bob.memberId), 503);
  assert.equal(f.core.session(bob.token).self.role, 'MEMBER');
});

test('malformed input, forged fields and invalid sequences fail closed without changing good data', () => {
  const f = fixture();
  const base = { location: point, stats: values, sharing: { locationRoles: ['FOUNDER'], statsRoles: [] } };
  f.publish(f.founder, base);
  for (const patch of [
    { sequence: 0 }, { sequence: 1.5 }, { sequence: '2' }, { sequence: Infinity },
    { teamId: 'other' }, { sharing: { locationRoles: ['OWNER'], statsRoles: [] } },
    { sharing: { locationRoles: ['MEMBER', 'MEMBER'], statsRoles: [] } },
    { context: { ...realm, extra: 1 } }, { context: null, location: point },
    { location: { ...point, x: NaN } }, { location: { ...point, x: 30000001 } },
    { stats: { ...values, hunger: 21 } }, { stats: { ...values, mainItem: '<script>' } }
  ]) denied(() => f.publish(f.founder, { ...base, ...patch }), 400);
  assert.deepEqual(from(f.core, f.founder, f.founder).location, point);
  denied(() => f.core.session(credential()), 401);
  denied(() => f.core.session(f.bootstrapToken), 401);
  denied(() => f.core.createTeam(f.founder.token, { teamName: 'No', displayName: 'No' }), 401);
  const invite = f.core.invite(f.founder.token);
  denied(() => f.core.join(invite.token, { displayName: 'Founder' }), 409);
  denied(() => f.core.join(invite.token, { displayName: 'Bad\nName' }), 400);
});

test('malformed stored membership cannot introduce extra founders, identities or private state', () => {
  const f = fixture(), clean = f.core.exportState();
  for (const mutate of [
    state => { state.version = 2; },
    state => { state.teams[0].members[0].role = 'MEMBER'; },
    state => { state.teams[0].members[0].location = point; },
    state => { state.teams[0].members.push({ ...state.teams[0].members[0] }); }
  ]) {
    const data = structuredClone(clean); mutate(data);
    denied(() => new RelayCore({ bootstrapToken: f.bootstrapToken, state: data }), 400);
  }
});
