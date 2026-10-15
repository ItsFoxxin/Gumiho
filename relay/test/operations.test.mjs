import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import net from 'node:net';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { createBackup, verifyBackup } from '../src/backup.mjs';
import { loadBootstrap, loadRelayConfig, normalizePublicHost } from '../src/config.mjs';
import { credential, RelayCore } from '../src/core.mjs';
import { healthcheck } from '../src/healthcheck.mjs';
import { listMembers, rotateMemberOffline } from '../src/operator.mjs';
import { membershipStore } from '../src/storage.mjs';

function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'gumiho-link-ops-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const bootstrapFile = path.join(root, 'private-bootstrap.json');
  const membershipFile = path.join(root, 'private-membership.json');
  const backupRoot = path.join(root, 'backups'); fs.mkdirSync(backupRoot, { mode: 0o700 });
  const bootstrapToken = credential('glb');
  fs.writeFileSync(bootstrapFile, JSON.stringify({ bootstrapToken }), { mode: 0o600 });
  const store = membershipStore(membershipFile);
  const core = new RelayCore({ bootstrapToken, persist: state => store.save(state) });
  const owner = core.createTeam(bootstrapToken, { teamName: 'Foxes', displayName: 'Owner' });
  const configFile = path.join(root, 'relay.json');
  fs.writeFileSync(configFile, JSON.stringify({ bootstrapFile, membershipFile, port: 18891,
    publicHosts: ['link.example.test'], trustProxy: true }));
  return { root, bootstrapFile, membershipFile, backupRoot, bootstrapToken, owner, configFile };
}

test('production config and private bootstrap parsing are strict', t => {
  const f = fixture(t), config = loadRelayConfig(f.configFile);
  assert.equal(config.port, 18891);
  assert.deepEqual(config.publicHosts, ['link.example.test']);
  assert.equal(loadBootstrap(f.bootstrapFile), f.bootstrapToken);
  assert.equal(normalizePublicHost('Link.Example.Test:8443'), 'link.example.test:8443');
  assert.throws(() => normalizePublicHost('*.example.test'));
  assert.throws(() => normalizePublicHost('https://example.test/path'));
  const bad = path.join(f.root, 'bad.json');
  fs.writeFileSync(bad, JSON.stringify({ bootstrapFile: 'relative', membershipFile: f.membershipFile,
    port: 8791, publicHosts: [], trustProxy: false }));
  assert.throws(() => loadRelayConfig(bad));
});

test('verified backups contain membership only and detect tampering', t => {
  const f = fixture(t), backup = createBackup(f.configFile, f.backupRoot);
  const verified = verifyBackup(backup);
  assert.equal(verified.hasMembership, true);
  const membership = fs.readFileSync(path.join(backup, 'private-membership.json'), 'utf8');
  assert.ok(!membership.includes(f.owner.token));
  assert.ok(!membership.includes('location'));
  fs.appendFileSync(path.join(backup, 'private-membership.json'), ' ');
  assert.throws(() => verifyBackup(backup), /hash mismatch/);
});

test('offline member recovery rotates a key without printing or weakening membership', async t => {
  const f = fixture(t);
  assert.equal(listMembers(f.configFile)[0].role, 'FOUNDER');
  const output = path.join(f.root, 'private-recovery.json');
  await rotateMemberOffline(f.configFile, f.owner.memberId, output, 'ROTATE_MEMBER_KEY');
  const replacement = JSON.parse(fs.readFileSync(output, 'utf8')).token;
  const restored = new RelayCore({ bootstrapToken: f.bootstrapToken, state: membershipStore(f.membershipFile).load() });
  assert.throws(() => restored.session(f.owner.token), failure => failure.status === 401);
  assert.equal(restored.session(replacement).self.role, 'FOUNDER');
  await assert.rejects(rotateMemberOffline(f.configFile, f.owner.memberId,
    path.join(f.root, 'bad-recovery.json'), 'wrong'));
});

test('production config starts loopback service and the bounded health checker accepts it', async t => {
  const f = fixture(t);
  const port = await new Promise((resolve, reject) => {
    const server = net.createServer(); server.once('error', reject);
    server.listen(0, '127.0.0.1', () => { const selected = server.address().port; server.close(() => resolve(selected)); });
  });
  const value = JSON.parse(fs.readFileSync(f.configFile, 'utf8')); value.port = port;
  fs.writeFileSync(f.configFile, JSON.stringify(value));
  const main = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../src/main.mjs');
  const child = spawn(process.execPath, [main, '--config', f.configFile], { stdio: ['ignore', 'pipe', 'pipe'] });
  let stopped = false;
  t.after(() => { if (!stopped) child.kill('SIGKILL'); });
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('production relay start timeout')), 5000);
    child.once('error', reject); child.stderr.on('data', data => reject(new Error(data.toString())));
    child.stdout.on('data', data => {
      if (data.toString().includes('production relay listening')) { clearTimeout(timer); resolve(); }
    });
  });
  assert.deepEqual(await healthcheck(f.configFile), { ok: true, protocol: 1 });
  child.kill('SIGTERM');
  const result = await new Promise(resolve => child.once('exit', (code, signal) => resolve({ code, signal }))); stopped = true;
  if (process.platform === 'win32') assert.equal(result.signal, 'SIGTERM');
  else assert.equal(result.code, 0);
});
