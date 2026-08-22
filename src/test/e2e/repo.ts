import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

// The test runner's working directory is not guaranteed, so anchor on pom.xml.
export function repoRoot(): string {
  let dir = resolve(process.cwd());
  while (!existsSync(join(dir, 'pom.xml'))) {
    const parent = dirname(dir);
    if (parent === dir) {
      throw new Error('Could not find the repository root: no pom.xml above ' + process.cwd());
    }
    dir = parent;
  }
  return dir;
}

export function runDir(): string {
  return join(repoRoot(), 'run');
}
