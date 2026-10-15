import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadRelayConfig } from './config.mjs';

export function healthcheck(configFile, timeout = 3000) {
  const config = loadRelayConfig(configFile), host = config.publicHosts[0] ?? `127.0.0.1:${config.port}`;
  return new Promise((resolve, reject) => {
    const request = http.get({ hostname: '127.0.0.1', port: config.port, path: '/health',
      headers: { Host: host }, timeout }, response => {
      let size = 0, body = '';
      response.setEncoding('utf8');
      response.on('data', chunk => { size += chunk.length; if (size <= 1024) body += chunk; });
      response.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          if (response.statusCode !== 200 || size > 1024 || parsed.ok !== true || parsed.protocol !== 1)
            throw new Error('Unhealthy relay response');
          resolve(parsed);
        } catch (failure) { reject(failure); }
      });
    });
    request.on('timeout', () => request.destroy(new Error('Health check timeout')));
    request.on('error', reject);
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  if (process.argv.length !== 3) throw new Error('Usage: node src/healthcheck.mjs /etc/gumiho-link/relay.json');
  await healthcheck(process.argv[2]);
  console.log('GUMIHO Link relay healthy (protocol 1)');
}
