import { test, expect, waitUntil } from '@drownek/plugwright';

test('a created home survives and is listed', async ({ player }) => {
  await player.makeOp();

  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  const since = player.getMessageBufferIndex();
  player.chat('/list-homes');
  await expect(player).toHaveReceivedMessage('base', { since });
});

test('the go-home command teleports the player back', async ({ player }) => {
  await player.makeOp();
  await player.setGameMode('spectator');

  const origin = player.bot.entity.position.clone();
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  player.chat('/home base');
  await expect(player).toHaveReceivedMessage('Teleported to base');
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) < 2);
});
