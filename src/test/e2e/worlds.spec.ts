import { test, expect, waitUntil } from '@drownek/plugwright';
import { standIn, isIn, OVERWORLD, NETHER, THE_END } from './dimensions.js';

test('a home can be created in the nether', async ({ player, server }) => {
  await player.makeOp();
  await standIn(server, player, NETHER);

  player.chat('/create-home hell');
  await expect(player).toHaveReceivedMessage('hell has been created successfully');
});

test('a home can be created in the end', async ({ player, server }) => {
  await player.makeOp();
  await standIn(server, player, THE_END);

  player.chat('/create-home void');
  await expect(player).toHaveReceivedMessage('void has been created successfully');
});

test('a nether home teleports the player back across dimensions', async ({ player, server }) => {
  await player.makeOp();
  await standIn(server, player, NETHER);
  player.chat('/create-home hell');
  await expect(player).toHaveReceivedMessage('hell has been created successfully');

  await standIn(server, player, OVERWORLD, 0, 100, 0);

  player.chat('/home hell');
  await waitUntil(() => isIn(player, NETHER), {
    timeout: 20000,
    message: 'player never returned to the nether',
  });
});

test('list-homes shows homes from every dimension', async ({ player, server }) => {
  await player.makeOp();

  await standIn(server, player, NETHER);
  player.chat('/create-home hell');
  await expect(player).toHaveReceivedMessage('hell has been created successfully');

  await standIn(server, player, THE_END);
  player.chat('/create-home void');
  await expect(player).toHaveReceivedMessage('void has been created successfully');

  const since = player.getMessageBufferIndex();
  player.chat('/list-homes');
  await expect(player).toHaveReceivedMessage('hell', { since });
  await expect(player).toHaveReceivedMessage('void', { since });
});
