import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const ignoredDirectories = new Set(['.git', '.gradle', 'build', 'node_modules', 'run', 'out', 'dist', 'artifacts']);
const violations = [];

function visit(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name);
    const relative = path.relative(root, absolute).replaceAll('\\', '/');
    if (entry.isDirectory()) {
      if (!ignoredDirectories.has(entry.name)) visit(absolute);
      continue;
    }

    if (/Name clash/i.test(entry.name)) violations.push(`${relative}: synchronization-conflict copy`);
    if (/\.log$/i.test(entry.name)) violations.push(`${relative}: generated log`);
    if (/^private-.*\.json$/i.test(entry.name)) violations.push(`${relative}: private relay state`);
    if (/^\.env(?:\..+)?$/i.test(entry.name) && entry.name !== '.env.example') {
      violations.push(`${relative}: environment/secrets file`);
    }
    if (/\.jar$/i.test(entry.name) && !relative.endsWith('/gradle/wrapper/gradle-wrapper.jar')) {
      violations.push(`${relative}: compiled JAR belongs in CI artifacts, not source control`);
    }
    if (fs.statSync(absolute).size > 25 * 1024 * 1024) violations.push(`${relative}: file exceeds 25 MiB`);
  }
}

visit(root);
if (violations.length) {
  console.error('Repository policy violations:');
  for (const violation of violations) console.error(`- ${violation}`);
  process.exit(1);
}
console.log('Repository source policy passed.');
