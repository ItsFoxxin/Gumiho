import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { membershipStore } from '../src/storage.mjs';
import { RelayCore, credential } from '../src/core.mjs';

test('atomic membership save/restore retains identities but not credentials or live telemetry', t => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'gumiho-link-test-'));
  t.after(() => {
    for (const filename of fs.readdirSync(directory)) fs.unlinkSync(path.join(directory, filename));
    fs.rmdirSync(directory);
  });
  const file = path.join(directory, 'private-membership.json'), store = membershipStore(file);
  assert.equal(store.load(), null);
  const bootstrapToken = credential('glb');
  const core = new RelayCore({ bootstrapToken, persist: state => store.save(state) });
  const owner = core.createTeam(bootstrapToken, { teamName: 'Team', displayName: 'Owner' });
  const invite = core.invite(owner.token);
  const bob = core.join(invite.token, { displayName: 'Bob' });
  core.publish(bob.token, { epoch: core.epoch, sequence: 1,
    context: { serverKey: 'a'.repeat(64), dimension: 'minecraft:overworld' },
    sharing: { locationRoles: ['FOUNDER'], statsRoles: [] }, location: { x: 3, y: 64, z: 7 } });
  const persisted = fs.readFileSync(file, 'utf8');
  assert.ok(!persisted.includes(bob.token));
  assert.ok(!persisted.includes(invite.token));
  assert.ok(!persisted.includes('location'));
  assert.ok(!persisted.includes('serverKey'));
  const restored = new RelayCore({ bootstrapToken, state: store.load() });
  assert.equal(restored.session(bob.token).self.name, 'Bob');
  assert.equal(restored.snapshot(owner.token).members.every(m => !m.online && m.location === null), true);
  assert.equal(fs.readdirSync(directory).length, 1);
});
