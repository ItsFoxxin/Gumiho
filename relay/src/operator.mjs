import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { credential, RelayCore } from './core.mjs';
import { loadBootstrap, loadRelayConfig } from './config.mjs';
import { membershipStore } from './storage.mjs';

const sha256 = value => createHash('sha256').update(value).digest('hex');
async function requireStopped(port) {
  await new Promise((resolve, reject) => {
    const request = http.get({ hostname: '127.0.0.1', port, path: '/health', timeout: 500 }, response => {
      response.resume(); reject(new Error('Stop gumiho-link.service before offline recovery'));
    });
    request.on('timeout', () => request.destroy());
    request.on('error', failure => failure.code === 'ECONNREFUSED' || failure.code === 'ECONNRESET' ? resolve() : reject(failure));
  });
}
function loaded(configFile) {
  const config = loadRelayConfig(configFile), bootstrapToken = loadBootstrap(config.bootstrapFile);
  const store = membershipStore(config.membershipFile), state = store.load();
  if (state === null) throw new Error('No membership state exists yet');
  new RelayCore({ bootstrapToken, state });
  return { config, bootstrapToken, store, state };
}
export function listMembers(configFile) {
  const { state } = loaded(configFile);
  return state.teams.flatMap(team => team.members.map(member => ({ teamId: team.id, team: team.name,
    memberId: member.id, name: member.name, role: member.role })));
}
export async function rotateMemberOffline(configFile, memberId, outputFile, confirmation) {
  if (confirmation !== 'ROTATE_MEMBER_KEY' || !/^[a-f0-9-]{36}$/.test(memberId)) throw new Error('Exact recovery confirmation required');
  const { config, bootstrapToken, store, state } = loaded(configFile);
  await requireStopped(config.port);
  const matches = state.teams.flatMap(team => team.members).filter(member => member.id === memberId);
  if (matches.length !== 1) throw new Error('Member not found');
  const token = credential(); matches[0].tokenHash = sha256(token);
  new RelayCore({ bootstrapToken, state });
  const target = path.resolve(outputFile);
  const pending = target + '.pending';
  if (fs.existsSync(target)) throw new Error('Recovery output already exists');
  fs.writeFileSync(pending, JSON.stringify({ memberId, token,
    warning: 'Private one-time recovery handoff. Delete this file after the member saves the key.' }, null, 2),
    { flag: 'wx', mode: 0o600 });
  try { store.save(state); } catch (failure) { fs.unlinkSync(pending); throw failure; }
  fs.renameSync(pending, target);
  return target;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const [command, configFile, memberId, outputFile, confirmation] = process.argv.slice(2);
  if (command === 'list' && configFile && !memberId) console.table(listMembers(configFile));
  else if (command === 'rotate-member' && configFile && memberId && outputFile)
    console.log('Replacement key written without printing it: ' + await rotateMemberOffline(configFile, memberId, outputFile, confirmation));
  else throw new Error('Usage: operator.mjs list <config> | rotate-member <config> <member-id> <output-file> ROTATE_MEMBER_KEY');
}
