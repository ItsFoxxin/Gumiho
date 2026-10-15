import http from 'node:http';
import { createHash } from 'node:crypto';
import { isIP } from 'node:net';
import { RelayError } from './core.mjs';

export const MAX_BODY = 16384;
const error = (status, code) => new RelayError(status, code);
class RateLimits {
  entries = new Map();
  constructor(clock) { this.clock = clock; }
  accept(key, limit) {
    const now = this.clock();
    for (const [id, value] of this.entries) if (value.until <= now) this.entries.delete(id);
    let entry = this.entries.get(key);
    if (!entry) {
      if (this.entries.size >= 2048) throw error(429, 'rate_limited');
      entry = { count: 0, until: now + 60000 }; this.entries.set(key, entry);
    }
    if (++entry.count > limit) throw error(429, 'rate_limited');
  }
}
function jsonBody(req) {
  if (req.headers['content-type']?.split(';')[0].trim().toLowerCase() !== 'application/json')
    return Promise.reject(error(415, 'json_required'));
  const declared = Number(req.headers['content-length'] ?? 0);
  if (!Number.isSafeInteger(declared) || declared < 0 || declared > MAX_BODY)
    return Promise.reject(error(413, 'body_too_large'));
  return new Promise((resolve, reject) => {
    let size = 0, finished = false, chunks = [];
    const stop = value => { if (!finished) { finished = true; chunks = []; reject(value); } };
    req.on('data', chunk => {
      if (finished) return;
      size += chunk.length;
      if (size > MAX_BODY) { stop(error(413, 'body_too_large')); return; }
      chunks.push(chunk);
    });
    req.on('end', () => {
      if (finished) return;
      finished = true;
      try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
      catch { reject(error(400, 'invalid_json')); }
    });
    req.on('error', () => stop(error(400, 'invalid_request')));
    req.on('aborted', () => stop(error(400, 'invalid_request')));
  });
}
const empty = body => {
  if (!body || Array.isArray(body) || typeof body !== 'object' || Object.keys(body).length)
    throw error(400, 'invalid_fields');
};
function token(req) {
  const count = req.rawHeaders.filter((value, index) => index % 2 === 0 && value.toLowerCase() === 'authorization').length;
  const value = req.headers.authorization;
  if (count !== 1 || typeof value !== 'string' || !/^Bearer gl(?:1|i|b)_[A-Za-z0-9_-]{43}$/.test(value))
    throw error(401, 'unauthorized');
  return value.slice(7);
}
function send(res, status, body) {
  if (res.destroyed || res.writableEnded) return;
  const text = JSON.stringify(body);
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(text), 'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer',
    ...(status >= 400 ? { Connection: 'close' } : {}),
    ...(status === 429 ? { 'Retry-After': '60' } : {}) });
  res.end(text);
}
const loopbackPeer = value => value === '127.0.0.1' || value === '::1' || value === '::ffff:127.0.0.1';
function requestPeer(req, trustProxy) {
  const direct = req.socket.remoteAddress ?? 'unknown';
  if (!trustProxy) return direct;
  if (!loopbackPeer(direct)) throw error(403, 'untrusted_proxy');
  const count = req.rawHeaders.filter((value, index) => index % 2 === 0 && value.toLowerCase() === 'x-forwarded-for').length;
  if (count === 0) return direct; // local health checks do not need a synthetic client address
  const value = req.headers['x-forwarded-for'];
  if (count !== 1 || typeof value !== 'string' || value.length > 256) throw error(400, 'invalid_forwarded_for');
  const first = value.split(',')[0].trim();
  if (!first || !isIP(first)) throw error(400, 'invalid_forwarded_for');
  return first;
}
/** No listener starts on import. The shipped CLI always binds only 127.0.0.1. */
export function createRelayServer(core, { clock = Date.now, allowedHosts = [], trustProxy = false } = {}) {
  const hosts = new Set(allowedHosts.map(host => String(host).toLowerCase()));
  const limits = new RateLimits(clock);
  const server = http.createServer({ maxHeaderSize: 8192, requestTimeout: 5000,
    headersTimeout: 5000, keepAliveTimeout: 1000 }, async (req, res) => {
    try {
      // Reject browser origins and DNS-rebinding hosts. Native clients use bearer headers.
      if (req.headers.origin !== undefined) throw error(403, 'browser_origin_not_allowed');
      const hostCount = req.rawHeaders.filter((value, index) => index % 2 === 0 && value.toLowerCase() === 'host').length;
      const requestedHost = typeof req.headers.host === 'string' ? req.headers.host.toLowerCase() : '';
      if (hostCount !== 1 || (!/^(127\.0\.0\.1|localhost|\[::1\])(?::[0-9]{1,5})?$/.test(requestedHost) && !hosts.has(requestedHost)))
        throw error(421, 'invalid_host');
      limits.accept('ip:' + requestPeer(req, trustProxy), 600);
      const url = new URL(req.url, 'http://127.0.0.1');
      if (url.search || req.url.length > 256) throw error(400, 'invalid_path');
      const path = url.pathname, method = req.method;
      if (path === '/health' && method === 'GET') { send(res, 200, { ok: true, protocol: 1 }); return; }
      const auth = token(req);
      limits.accept('token:' + createHash('sha256').update(auth).digest('hex'), 360);
      if (path === '/v1/teams' && method === 'POST') {
        send(res, 201, core.createTeam(auth, await jsonBody(req))); return;
      }
      if (path === '/v1/join' && method === 'POST') {
        send(res, 201, core.join(auth, await jsonBody(req))); return;
      }
      if (path === '/v1/session' && method === 'GET') { send(res, 200, core.session(auth)); return; }
      if (path === '/v1/snapshot' && method === 'GET') { send(res, 200, core.snapshot(auth)); return; }
      if (path === '/v1/me/status' && method === 'PUT') {
        send(res, 200, core.publish(auth, await jsonBody(req))); return;
      }
      if (path === '/v1/me/disconnect' && method === 'POST') {
        send(res, 200, core.disconnect(auth, await jsonBody(req))); return;
      }
      if (path === '/v1/me/token' && method === 'POST') {
        empty(await jsonBody(req)); send(res, 200, core.rotateToken(auth)); return;
      }
      if (path === '/v1/team/founder' && method === 'PUT') {
        send(res, 200, core.transferFounder(auth, await jsonBody(req))); return;
      }
      if (path === '/v1/invites' && method === 'POST') {
        empty(await jsonBody(req)); send(res, 201, core.invite(auth)); return;
      }
      let match = path.match(/^\/v1\/invites\/([a-f0-9-]{36})$/);
      if (match && method === 'DELETE') { send(res, 200, core.revokeInvite(auth, match[1])); return; }
      match = path.match(/^\/v1\/members\/([a-f0-9-]{36})\/role$/);
      if (match && method === 'PUT') { send(res, 200, core.setRole(auth, match[1], await jsonBody(req))); return; }
      match = path.match(/^\/v1\/members\/([a-f0-9-]{36})$/);
      if (match && method === 'DELETE') { send(res, 200, core.remove(auth, match[1])); return; }
      if (path === '/v1/pings' && method === 'POST') {
        send(res, 201, core.ping(auth, await jsonBody(req))); return;
      }
      if (path === '/v1/me/pings' && method === 'DELETE') { send(res, 200, core.clearPings(auth)); return; }
      throw error(404, 'not_found');
    } catch (failure) {
      // Do not log request bodies, headers, bearer tokens, locations or raw exceptions.
      send(res, failure instanceof RelayError ? failure.status : 500,
        { error: failure instanceof RelayError ? failure.code : 'internal_error' });
    }
  });
  server.maxConnections = 64;
  server.maxHeadersCount = 32;
  server.maxRequestsPerSocket = 100;
  server.on('clientError', (_failure, socket) => {
    if (socket.writable) socket.end('HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n');
  });
  const timer = setInterval(() => core.sweep(), 1000).unref();
  server.once('close', () => clearInterval(timer));
  return server;
}
