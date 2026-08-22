import { waitUntil, type PlayerWrapper, type ServerWrapper } from '@drownek/plugwright';

export const OVERWORLD = 'world';
export const NETHER = 'world_nether';
export const THE_END = 'world_the_end';

const DIMENSION_KEY: Record<string, string> = {
  [OVERWORLD]: 'overworld',
  [NETHER]: 'the_nether',
  [THE_END]: 'the_end',
};

// mineflayer types bot.game.dimension as the bare key, but the server sends it
// namespaced, so accept either form.
export function isIn(player: PlayerWrapper, world: string): boolean {
  const key = DIMENSION_KEY[world];
  const current = String(player.bot.game.dimension);
  return current === key || current === `minecraft:${key}`;
}

/**
 * Puts the player on solid ground in the given world and waits for the client to
 * report the change.
 *
 * The generator decides what is at any given spot in the nether and the end, and
 * teleportSafety refuses a home inside blocks, so the pocket is cleared and
 * floored first.
 */
export async function standIn(
  server: ServerWrapper,
  player: PlayerWrapper,
  world: string,
  x = 64,
  y = 100,
  z = 64,
): Promise<void> {
  const dimension = `minecraft:${DIMENSION_KEY[world]}`;

  server.execute(`minecraft:execute in ${dimension} run fill ${x - 3} ${y - 1} ${z - 3} ${x + 3} ${y + 4} ${z + 3} minecraft:air`);
  server.execute(`minecraft:execute in ${dimension} run fill ${x - 3} ${y - 1} ${z - 3} ${x + 3} ${y - 1} ${z + 3} minecraft:stone`);
  server.execute(`minecraft:execute in ${dimension} run tp ${player.username} ${x} ${y} ${z}`);

  await waitUntil(() => isIn(player, world), {
    timeout: 20000,
    message: `player never arrived in ${world}`,
  });
}
