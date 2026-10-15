import fs from 'node:fs';
import path from 'node:path';
import { credential, RelayCore } from './core.mjs';
import { createRelayServer } from './http.mjs';
import { membershipStore } from './storage.mjs';
import { loadBootstrap, loadRelayConfig } from './config.mjs';

function run({ bootstrapFile, membershipFile, port, publicHosts = [], trustProxy = false, mode = 'development' }) {
  const bootstrapToken = loadBootstrap(bootstrapFile), store = membershipStore(membershipFile);
  const core = new RelayCore({ bootstrapToken, state: store.load(), persist: state => store.save(state) });
  const server = createRelayServer(core, { allowedHosts: publicHosts, trustProxy });
  server.listen(port, '127.0.0.1', () => console.log(`GUMIHO Link ${mode} relay listening on loopback port ${port}`));
  server.on('error', failure => { console.error('Relay listener failed:', failure.code ?? 'unknown'); process.exitCode = 1; });
  let stopping = false;
  for (const signal of ['SIGINT', 'SIGTERM']) process.once(signal, () => {
    if (stopping) return; stopping = true;
    server.close(() => { process.exitCode = 0; }); server.closeAllConnections();
  });
}

const args = process.argv.slice(2);
if (args[0] === '--init' && args.length === 2) {
  const target = path.resolve(args[1]);
  fs.writeFileSync(target, JSON.stringify({ bootstrapToken: credential('glb') }, null, 2), { flag: 'wx', mode: 0o600 });
  console.log('Created private bootstrap file. Keep it restricted; never share it with members.');
} else if (args[0] === '--config' && args.length === 2) {
  const config = loadRelayConfig(args[1]);
  run({ ...config, mode: 'production' });
} else if (args.length === 2 || args.length === 3) {
  const [bootstrapFile, stateFile, portText = '8791'] = args;
  const port = Number(portText);
  if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error('Use a port from 1024 to 65535');
  run({ bootstrapFile, membershipFile: stateFile, port, mode: 'development' });
} else {
  console.log('Initialize: node src/main.mjs --init private-bootstrap.json');
  console.log('Run locally: node src/main.mjs private-bootstrap.json private-membership.json [8791]');
  console.log('Run service: node src/main.mjs --config /etc/gumiho-link/relay.json');
  console.log('The Node listener is always loopback-only; use an HTTPS reverse proxy for remote clients.');
  process.exitCode = 1;
}
