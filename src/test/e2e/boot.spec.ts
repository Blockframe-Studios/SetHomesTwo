import { test, expect } from '@drownek/plugwright';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { runDir } from './repo.js';

test('the plugin creates its SQLite database on a real server', async ({ player }) => {
  expect(player.username).toBeTruthy();

  const database = join(runDir(), 'plugins', 'SetHomesTwo', 'database', 'homes.db');
  expect(existsSync(database)).toBe(true);
});

test('the plugin starts without logging an error', async ({ player }) => {
  expect(player.username).toBeTruthy();

  const log = readFileSync(join(runDir(), 'logs', 'latest.log'), 'utf8');
  const bad = log
    .split('\n')
    // Paper renders levels as "[Thread/ERROR]", not the plain "SEVERE" token
    // java.util.logging uses.
    .filter(line => /\/(ERROR|SEVERE)\]|Could not load|Error occurred while enabling/.test(line))
    // Bukkit.getLogger() is the root logger, so a plugin-logged error carries
    // the "SH2" prefix set in plugin.yml, not the plugin's class/jar name.
    .filter(line => line.includes('SetHomesTwo') || line.includes('[SH2]'));

  expect(bad).toEqual([]);
});
