// Private child-process test fixture (not an auto-discovered Node test).
import { RelayCore, credential } from '../src/core.mjs';
import { createRelayServer } from '../src/http.mjs';
const bootstrap = credential('glb');
const server = createRelayServer(new RelayCore({ bootstrapToken: bootstrap }));
server.listen(0, '127.0.0.1', () => {
  process.stdout.write(JSON.stringify({ endpoint: `http://127.0.0.1:${server.address().port}`, bootstrap }) + '\n');
});
process.stdin.resume();
process.stdin.on('end', () => { server.close(); server.closeAllConnections(); });
