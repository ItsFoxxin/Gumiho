import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const canonical = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim();
const semver = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-(?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*)(?:\.(?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*))*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;

if (!semver.test(canonical)) {
  throw new Error(`VERSION is not valid Semantic Versioning: ${canonical}`);
}

function gradleVersion(relativePath) {
  const body = fs.readFileSync(path.join(root, relativePath), 'utf8');
  const match = body.match(/^mod_version\s*=\s*(.+)$/m);
  if (!match) throw new Error(`${relativePath} has no mod_version`);
  return match[1].trim();
}

const declarations = new Map([
  ['clients/mc1.21.1/gradle.properties', gradleVersion('clients/mc1.21.1/gradle.properties')],
  ['clients/mc26.2/gradle.properties', gradleVersion('clients/mc26.2/gradle.properties')],
  ['relay/package.json', JSON.parse(fs.readFileSync(path.join(root, 'relay/package.json'), 'utf8')).version]
]);

let failed = false;
for (const [file, declared] of declarations) {
  if (declared !== canonical) {
    console.error(`${file}: expected ${canonical}, found ${declared}`);
    failed = true;
  } else {
    console.log(`ok ${file} -> ${declared}`);
  }
}

if (failed) process.exit(1);
console.log(`Version contract passed: ${canonical}`);
