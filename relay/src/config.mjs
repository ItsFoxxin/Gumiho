import fs from 'node:fs';
import path from 'node:path';

const exactObject = (value, keys, label) => {
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).length !== keys.length || keys.some(key => !Object.hasOwn(value, key)))
    throw new Error('Invalid ' + label);
  return value;
};
const readSmallJson = (filename, maximum, label) => {
  const target = path.resolve(filename), stat = fs.lstatSync(target);
  if (!stat.isFile() || stat.isSymbolicLink() || stat.size > maximum) throw new Error('Invalid ' + label);
  return { target, stat, value: JSON.parse(fs.readFileSync(target, 'utf8')) };
};
const privateMode = ({ stat }, label) => {
  if (process.platform !== 'win32' && (stat.mode & 0o077) !== 0)
    throw new Error(label + ' must not be readable by group or other users');
};

export function normalizePublicHost(value) {
  if (typeof value !== 'string' || value !== value.trim() || value.length < 1 || value.length > 253 || value.includes('*'))
    throw new Error('Invalid public host');
  let parsed;
  try { parsed = new URL('http://' + value); } catch { throw new Error('Invalid public host'); }
  if (parsed.username || parsed.password || parsed.pathname !== '/' || parsed.search || parsed.hash || !parsed.hostname)
    throw new Error('Invalid public host');
  return parsed.host.toLowerCase();
}

export function loadRelayConfig(filename) {
  const { value } = readSmallJson(filename, 16384, 'relay config');
  exactObject(value, ['bootstrapFile', 'membershipFile', 'port', 'publicHosts', 'trustProxy'], 'relay config');
  for (const key of ['bootstrapFile', 'membershipFile'])
    if (typeof value[key] !== 'string' || !path.isAbsolute(value[key])) throw new Error(key + ' must be an absolute path');
  if (!Number.isInteger(value.port) || value.port < 1024 || value.port > 65535) throw new Error('Invalid relay port');
  if (!Array.isArray(value.publicHosts) || value.publicHosts.length > 8) throw new Error('Invalid publicHosts');
  const publicHosts = value.publicHosts.map(normalizePublicHost);
  if (new Set(publicHosts).size !== publicHosts.length) throw new Error('Duplicate public host');
  if (typeof value.trustProxy !== 'boolean') throw new Error('Invalid trustProxy');
  return Object.freeze({ configFile: path.resolve(filename), bootstrapFile: path.resolve(value.bootstrapFile),
    membershipFile: path.resolve(value.membershipFile), port: value.port,
    publicHosts: Object.freeze(publicHosts), trustProxy: value.trustProxy });
}

export function loadBootstrap(filename) {
  const source = readSmallJson(filename, 4096, 'bootstrap file');
  privateMode(source, 'Bootstrap file');
  exactObject(source.value, ['bootstrapToken'], 'bootstrap file');
  if (typeof source.value.bootstrapToken !== 'string' || !/^glb_[A-Za-z0-9_-]{43}$/.test(source.value.bootstrapToken))
    throw new Error('Invalid bootstrap file');
  return source.value.bootstrapToken;
}
