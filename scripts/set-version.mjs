import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const next = process.argv[2];
const semver = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-(?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*)(?:\.(?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*))*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;
if (!next || !semver.test(next)) {
  console.error('Usage: node scripts/set-version.mjs <semver>');
  console.error('Example: node scripts/set-version.mjs 1.0.0-rc.1');
  process.exit(2);
}

const root = path.resolve(import.meta.dirname, '..');
fs.writeFileSync(path.join(root, 'VERSION'), `${next}\n`);

for (const relative of ['clients/mc1.21.1/gradle.properties', 'clients/mc26.2/gradle.properties']) {
  const file = path.join(root, relative);
  const current = fs.readFileSync(file, 'utf8');
  if (!/^mod_version\s*=.*$/m.test(current)) throw new Error(`${relative} has no mod_version`);
  fs.writeFileSync(file, current.replace(/^mod_version\s*=.*$/m, `mod_version=${next}`));
}

const packageFile = path.join(root, 'relay/package.json');
const relayPackage = JSON.parse(fs.readFileSync(packageFile, 'utf8'));
relayPackage.version = next;
fs.writeFileSync(packageFile, `${JSON.stringify(relayPackage, null, 2)}\n`);

console.log(`Updated the repository version to ${next}.`);
console.log('Run: node scripts/check-version.mjs');
