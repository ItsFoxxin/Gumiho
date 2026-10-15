import test from 'node:test';
import http from 'node:http';
import assert from 'node:assert/strict';
import { RelayCore, credential } from '../src/core.mjs';
import { createRelayServer, MAX_BODY } from '../src/http.mjs';

async function service(t, serverOptions = {}) {
  const bootstrap = credential('glb'), core = new RelayCore({ bootstrapToken: bootstrap });
  const server = createRelayServer(core, serverOptions);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => new Promise(resolve => { server.close(resolve); server.closeAllConnections(); }));
  const url = 'http://127.0.0.1:' + server.address().port;
  const call = async (path, { method = 'GET', token, body, headers = {}, raw } = {}) => {
    const response = await fetch(url + path, { method, headers: {
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
      ...(body !== undefined || raw !== undefined ? { 'Content-Type': 'application/json' } : {}), ...headers },
      body: raw ?? (body === undefined ? undefined : JSON.stringify(body)),
      signal: AbortSignal.timeout(3000) });
    return { status: response.status, body: await response.json(), headers: response.headers };
  };
  return { core, bootstrap, call, url };
}
const scope = { serverKey: '1'.repeat(64), dimension: 'minecraft:overworld' };

test('two authenticated HTTP clients exchange only consented fields, pings and revocation updates', async t => {
  const { call, bootstrap } = await service(t);
  const created = await call('/v1/teams', { method: 'POST', token: bootstrap,
    body: { teamName: 'Foxes', displayName: 'Alice' } });
  assert.equal(created.status, 201);
  const alice = created.body;
  const invitation = await call('/v1/invites', { method: 'POST', token: alice.token, body: {} });
  assert.equal(invitation.status, 201);
  const joined = await call('/v1/join', { method: 'POST', token: invitation.body.token, body: { displayName: 'Bob' } });
  assert.equal(joined.status, 201);
  const bob = joined.body;
  const session = await call('/v1/session', { token: alice.token });
  const epoch = session.body.epoch;
  const publish = (token, sequence, extra = {}) => call('/v1/me/status', { method: 'PUT', token,
    body: { epoch, sequence, context: scope, sharing: { locationRoles: [], statsRoles: [] }, ...extra } });
  assert.equal((await publish(bob.token, 1)).status, 200);
  assert.equal((await publish(alice.token, 1, { location: { x: 1, y: 64, z: 2 } })).status, 200);
  let snapshot = await call('/v1/snapshot', { token: bob.token });
  assert.equal(snapshot.body.members.find(m => m.id === alice.memberId).location, null);
  assert.equal(snapshot.headers.get('cache-control'), 'no-store');
  assert.equal(snapshot.headers.get('access-control-allow-origin'), null);
  assert.equal((await publish(alice.token, 2, { location: { x: 1, y: 64, z: 2 },
    sharing: { locationRoles: ['MEMBER'], statsRoles: [] } })).status, 200);
  snapshot = await call('/v1/snapshot', { token: bob.token });
  assert.deepEqual(snapshot.body.members.find(m => m.id === alice.memberId).location, { x: 1, y: 64, z: 2 });
  const ping = await call('/v1/pings', { method: 'POST', token: alice.token,
    body: { epoch, context: scope, position: { x: 10, y: 64, z: 20 }, kind: 'HELP', audienceRoles: ['MEMBER'] } });
  assert.equal(ping.status, 201);
  assert.equal((await call('/v1/snapshot', { token: bob.token })).body.pings.length, 1);
  assert.equal((await call('/v1/me/disconnect', { method: 'POST', token: alice.token, body: { epoch, sequence: 3 } })).status, 200);
  snapshot = await call('/v1/snapshot', { token: bob.token });
  assert.equal(snapshot.body.pings.length, 0);
  assert.equal(snapshot.body.members.find(m => m.id === alice.memberId).online, false);
  assert.equal((await publish(alice.token, 2)).status, 409);
  assert.equal((await call('/v1/members/' + bob.memberId, { method: 'DELETE', token: alice.token })).status, 200);
  assert.equal((await call('/v1/snapshot', { token: bob.token })).status, 401);
});

test('HTTP rejects missing authentication, browser origins, query secrets, wrong JSON and oversized bodies', async t => {
  const { call, bootstrap } = await service(t);
  assert.equal((await call('/health')).status, 200);
  assert.equal((await call('/v1/session')).status, 401);
  assert.equal((await call('/v1/session', { token: credential() })).status, 401);
  assert.equal((await call('/health', { headers: { Origin: 'https://untrusted.example' } })).status, 403);
  assert.equal((await call('/v1/session?token=secret', { token: bootstrap })).status, 400);
  const request = { method: 'POST', token: bootstrap };
  assert.equal((await call('/v1/teams', { ...request, raw: 'not-json' })).status, 400);
  assert.equal((await call('/v1/teams', { ...request, raw: '{}', headers: { 'Content-Type': 'text/plain' } })).status, 415);
  assert.equal((await call('/v1/teams', { ...request, raw: 'x'.repeat(MAX_BODY + 1) })).status, 413);
  assert.equal((await call('/v1/teams', { ...request, body: { teamName: 'Fox', displayName: 'Alice', role: 'FOUNDER' } })).status, 400);
});

test('HTTP role/ping inputs cannot be used to access another team', async t => {
  const { call, bootstrap } = await service(t);
  const create = name => call('/v1/teams', { method: 'POST', token: bootstrap, body: { teamName: name, displayName: name } });
  const a = (await create('A')).body, b = (await create('B')).body;
  assert.equal((await call('/v1/members/' + b.memberId + '/role', { method: 'PUT', token: a.token, body: { role: 'MEMBER' } })).status, 404);
  assert.equal((await call('/v1/snapshot', { token: a.token })).body.members.length, 1);
  const rotate = await call('/v1/me/token', { method: 'POST', token: a.token, body: {} });
  assert.equal(rotate.status, 200);
  assert.equal((await call('/v1/session', { token: a.token })).status, 401);
  assert.equal((await call('/v1/session', { token: rotate.body.token })).status, 200);
});

test('HTTP Founder transfer requires Founder, exact confirmation and eligible target', async t => {
  const { call, bootstrap } = await service(t);
  const owner = (await call('/v1/teams', { method: 'POST', token: bootstrap,
    body: { teamName: 'Fox', displayName: 'Owner' } })).body;
  const invitation = (await call('/v1/invites', { method: 'POST', token: owner.token, body: {} })).body;
  const member = (await call('/v1/join', { method: 'POST', token: invitation.token,
    body: { displayName: 'Member' } })).body;
  assert.equal((await call('/v1/team/founder', { method: 'PUT', token: member.token,
    body: { memberId: owner.memberId, confirmation: 'TRANSFER_FOUNDER' } })).status, 403);
  assert.equal((await call('/v1/team/founder', { method: 'PUT', token: owner.token,
    body: { memberId: member.memberId, confirmation: 'wrong' } })).status, 403);
  assert.equal((await call('/v1/team/founder', { method: 'PUT', token: owner.token,
    body: { memberId: member.memberId, confirmation: 'TRANSFER_FOUNDER' } })).status, 200);
  assert.equal((await call('/v1/session', { token: member.token })).body.self.role, 'FOUNDER');
});

test('HTTP host validation and bounded request rate are enforced', async t => {
  const { call, url } = await service(t);
  // fetch normalizes Host; use a raw HTTP request to test a real forged header.
  const status = await new Promise((resolve, reject) => {
    const req = http.get(url + '/health', { headers: { Host: 'untrusted.example' } }, res => {
      res.resume(); res.on('end', () => resolve(res.statusCode));
    });
    req.on('error', reject);
    req.setTimeout(3000, () => req.destroy(new Error('test timeout')));
  });
  assert.equal(status, 421);
  for (let i = 0; i < 600; i++) assert.equal((await call('/health')).status, 200);
  const limited = await call('/health');
  assert.equal(limited.status, 429);
  assert.equal(limited.headers.get('retry-after'), '60');
});

test('configured reverse-proxy host is exact and forwarded client addresses are validated', async t => {
  const { url } = await service(t, { allowedHosts: ['link.example.test'], trustProxy: true });
  const raw = headers => new Promise((resolve, reject) => {
    const req = http.get(url + '/health', { headers }, res => {
      res.resume(); res.on('end', () => resolve(res.statusCode));
    });
    req.on('error', reject); req.setTimeout(3000, () => req.destroy(new Error('test timeout')));
  });
  assert.equal(await raw({ Host: 'link.example.test', 'X-Forwarded-For': '203.0.113.44' }), 200);
  assert.equal(await raw({ Host: 'LINK.EXAMPLE.TEST', 'X-Forwarded-For': '2001:db8::44' }), 200);
  assert.equal(await raw({ Host: 'other.example.test', 'X-Forwarded-For': '203.0.113.44' }), 421);
  assert.equal(await raw({ Host: 'link.example.test', 'X-Forwarded-For': 'not-an-ip' }), 400);
});
