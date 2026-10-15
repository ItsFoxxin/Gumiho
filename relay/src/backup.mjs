import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { RelayCore } from './core.mjs';
import { loadBootstrap, loadRelayConfig } from './config.mjs';
import { membershipStore } from './storage.mjs';

const sha256 = value => createHash('sha256').update(value).digest('hex');
const privateWrite = (filename, value) => fs.writeFileSync(filename, value, { flag: 'wx', mode: 0o600 });
const directory = filename => {
  const stat = fs.lstatSync(filename);
  if (!stat.isDirectory() || stat.isSymbolicLink()) throw new Error('Backup destination must be a real directory');
};

export function verifyBackup(backupDirectory) {
  const root = path.resolve(backupDirectory); directory(root);
  const manifestFile = path.join(root, 'manifest.json'), manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));
  if (!manifest || Object.keys(manifest).sort().join(',') !== 'bootstrap,createdAt,membership,protocol' ||
      manifest.protocol !== 1 || manifest.bootstrap?.file !== 'private-bootstrap.json' ||
      !/^[a-f0-9]{64}$/.test(manifest.bootstrap?.sha256 ?? '')) throw new Error('Invalid backup manifest');
  const bootstrapFile = path.join(root, manifest.bootstrap.file), bootstrapBytes = fs.readFileSync(bootstrapFile);
  if (sha256(bootstrapBytes) !== manifest.bootstrap.sha256) throw new Error('Bootstrap backup hash mismatch');
  const bootstrapToken = loadBootstrap(bootstrapFile);
  let state = null;
  if (manifest.membership !== null) {
    if (Object.keys(manifest.membership).sort().join(',') !== 'file,sha256' ||
        manifest.membership.file !== 'private-membership.json' || !/^[a-f0-9]{64}$/.test(manifest.membership.sha256))
      throw new Error('Invalid membership backup manifest');
    const membershipFile = path.join(root, manifest.membership.file), bytes = fs.readFileSync(membershipFile);
    if (sha256(bytes) !== manifest.membership.sha256) throw new Error('Membership backup hash mismatch');
    state = membershipStore(membershipFile).load();
  }
  new RelayCore({ bootstrapToken, state });
  return Object.freeze({ root, createdAt: manifest.createdAt, hasMembership: state !== null });
}

export function createBackup(configFile, destinationRoot) {
  const config = loadRelayConfig(configFile); directory(destinationRoot);
  const bootstrapToken = loadBootstrap(config.bootstrapFile), store = membershipStore(config.membershipFile);
  const state = store.load(); new RelayCore({ bootstrapToken, state });
  const stamp = new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-');
  const target = path.join(path.resolve(destinationRoot), 'gumiho-link-' + stamp);
  fs.mkdirSync(target, { mode: 0o700 });
  try {
    const bootstrapBytes = fs.readFileSync(config.bootstrapFile);
    privateWrite(path.join(target, 'private-bootstrap.json'), bootstrapBytes);
    let membership = null;
    if (state !== null) {
      const membershipBytes = fs.readFileSync(config.membershipFile);
      privateWrite(path.join(target, 'private-membership.json'), membershipBytes);
      membership = { file: 'private-membership.json', sha256: sha256(membershipBytes) };
    }
    const manifest = { protocol: 1, createdAt: new Date().toISOString(),
      bootstrap: { file: 'private-bootstrap.json', sha256: sha256(bootstrapBytes) }, membership };
    privateWrite(path.join(target, 'manifest.json'), JSON.stringify(manifest, null, 2));
    verifyBackup(target);
    return target;
  } catch (failure) {
    for (const name of fs.readdirSync(target)) fs.unlinkSync(path.join(target, name));
    fs.rmdirSync(target); throw failure;
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const [command, first, second] = process.argv.slice(2);
  if (command === 'create' && first && second) console.log('Verified backup created at ' + createBackup(first, second));
  else if (command === 'verify' && first && !second) console.log('Backup verified: ' + verifyBackup(first).root);
  else throw new Error('Usage: backup.mjs create <relay-config> <destination-dir> | verify <backup-dir>');
}
