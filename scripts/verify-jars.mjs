import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const [projectArgument, minecraftTarget] = process.argv.slice(2);
if (!projectArgument || !minecraftTarget) {
  console.error('Usage: node scripts/verify-jars.mjs <project-directory> <minecraft-target>');
  process.exit(2);
}

const root = path.resolve(import.meta.dirname, '..');
const project = path.resolve(root, projectArgument);
const version = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim();
const jars = [
  {
    kind: 'full',
    path: path.join(project, 'build', 'libs', `gumiho-neoforge-${version}.jar`),
    required: ['META-INF/neoforge.mods.toml', 'dev/gumiho/client/GumihoClient.class', 'dev/gumiho/link/LinkClient.class'],
    forbidden: []
  },
  {
    kind: 'link-only',
    path: path.join(project, 'build', 'libs', `gumiho-link-${version}.jar`),
    required: ['META-INF/neoforge.mods.toml', 'dev/gumiho/link/LinkClient.class', 'assets/gumiho_link/lang/en_us.json'],
    forbidden: ['dev/gumiho/client/', 'assets/gumiho/']
  }
];

for (const jar of jars) {
  if (!fs.existsSync(jar.path)) throw new Error(`Missing ${jar.kind} JAR: ${jar.path}`);
  const listing = spawnSync('jar', ['tf', jar.path], { encoding: 'utf8' });
  if (listing.status !== 0) throw new Error(`Could not inspect ${jar.path}: ${listing.stderr}`);
  const entries = new Set(listing.stdout.split(/\r?\n/).filter(Boolean));
  for (const required of jar.required) {
    if (!entries.has(required)) throw new Error(`${jar.kind} JAR is missing ${required}`);
  }
  for (const forbiddenPrefix of jar.forbidden) {
    if ([...entries].some((entry) => entry.startsWith(forbiddenPrefix))) {
      throw new Error(`${jar.kind} JAR unexpectedly contains ${forbiddenPrefix}`);
    }
  }
  if ([...entries].some((entry) => /Name clash/i.test(entry))) {
    throw new Error(`${jar.kind} JAR contains a synchronization-conflict copy`);
  }
  console.log(`ok ${jar.kind} ${minecraftTarget}: ${path.basename(jar.path)} (${entries.size} entries)`);
}
