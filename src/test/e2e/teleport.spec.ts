import { test, expect, waitUntil, waitForStable, sleep } from '@drownek/plugwright';

// The harness config sets a two second delay with cancel-on-move and safety on.
// An op holds sh2.bypass-teleport-delay and takes neither of the first two.
//
// The move check compares the stored location to the current one with exact
// equality, so these tests stand still in spectator, where there is no physics
// to nudge the position.

test('an ordinary player waits out the countdown before arriving', async ({ player }) => {
  await player.setGameMode('spectator');
  const origin = player.bot.entity.position.clone();

  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  // The move check is exact, and an idle bot still sends position packets that
  // differ in the last decimal, which reads as movement and cancels the run.
  player.bot.physicsEnabled = false;

  player.chat('/home base');
  await waitForStable(() => player.bot.entity.position.distanceTo(origin) > 20, { duration: 1200 });
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) < 4, { timeout: 15000 });
});

test('an op holding the bypass arrives without the countdown', async ({ player }) => {
  await player.makeOp();
  await player.setGameMode('spectator');
  const origin = player.bot.entity.position.clone();

  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  player.chat('/home base');
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) < 4, { timeout: 1500 });
});

test('moving during the countdown cancels the teleport', async ({ player }) => {
  await player.setGameMode('spectator');
  const origin = player.bot.entity.position.clone();

  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  player.chat('/home base');
  await sleep(300);
  await player.teleport(origin.x + 70, origin.y, origin.z + 70);

  await expect(player).toHaveReceivedMessage('Your teleport has been canceled because you have moved', { timeout: 15000 });
  await waitForStable(() => player.bot.entity.position.distanceTo(origin) > 20, { duration: 1500 });
});

test('a second teleport while one is pending is refused', async ({ player }) => {
  await player.setGameMode('spectator');
  const origin = player.bot.entity.position.clone();

  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  player.bot.physicsEnabled = false;

  player.chat('/home base');
  await sleep(250);
  const since = player.getMessageBufferIndex();
  player.chat('/home base');

  await expect(player).toHaveReceivedMessage('You cannot teleport while already teleporting', { since, timeout: 10000 });
});

test('a home sealed in blocks is relocated or refused', async ({ player, server }) => {
  await player.makeOp();
  await player.setGameMode('spectator');
  const origin = player.bot.entity.position.clone();

  player.chat('/create-home buried');
  await expect(player).toHaveReceivedMessage('buried has been created successfully');

  const x = Math.floor(origin.x);
  const y = Math.floor(origin.y);
  const z = Math.floor(origin.z);
  server.execute(`minecraft:fill ${x - 1} ${y} ${z - 1} ${x + 1} ${y + 2} ${z + 1} minecraft:stone`);
  await sleep(750);

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  player.chat('/home buried');
  await expect(player).toHaveReceivedMessage(/not safe to stand in|nearest safe spot/, { timeout: 15000 });
});
