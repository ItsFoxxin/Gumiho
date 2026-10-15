import fs from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';

/** Membership only. Caller validates schema; telemetry is never passed to this store. */
export function membershipStore(filename) {
  const target = path.resolve(filename);
  const validateTarget = () => {
    const stat = fs.lstatSync(target);
    if (!stat.isFile() || stat.isSymbolicLink() || stat.size > 2097152) throw new Error('Invalid membership store');
    if (process.platform !== 'win32' && (stat.mode & 0o077) !== 0)
      throw new Error('Membership store must not be readable by group or other users');
    return stat;
  };
  return {
    load() {
      if (!fs.existsSync(target)) return null;
      validateTarget();
      return JSON.parse(fs.readFileSync(target, 'utf8'));
    },
    save(state) {
      if (fs.existsSync(target)) validateTarget();
      const temp = target + '.' + randomUUID() + '.tmp';
      let handle;
      try {
        handle = fs.openSync(temp, 'wx', 0o600);
        fs.writeFileSync(handle, JSON.stringify(state), 'utf8');
        fs.fsyncSync(handle); fs.closeSync(handle); handle = undefined;
        fs.renameSync(temp, target);
        if (process.platform !== 'win32') {
          const directory = fs.openSync(path.dirname(target), 'r');
          try { fs.fsyncSync(directory); } finally { fs.closeSync(directory); }
        }
      } finally {
        if (handle !== undefined) fs.closeSync(handle);
        if (fs.existsSync(temp)) fs.unlinkSync(temp);
      }
    }
  };
}
