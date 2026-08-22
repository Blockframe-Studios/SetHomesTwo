import { test, expect } from '@drownek/plugwright';
import { standIn, NETHER } from './dimensions.js';

// The blacklist table is server-wide, so every test here removes what it adds.

test('a dimension can be blacklisted, listed and removed', async ({ player }) => {
  await player.makeOp();

  player.chat('/blacklist add world_nether');
  await expect(player).toHaveReceivedMessage('world_nether has been added to the blacklist');

  const listed = player.getMessageBufferIndex();
  player.chat('/blacklist list');
  await expect(player).toHaveReceivedMessage('world_nether', { since: listed });

  player.chat('/blacklist remove world_nether');
  await expect(player).toHaveReceivedMessage('world_nether has been removed from the blacklist');

  const emptied = player.getMessageBufferIndex();
  player.chat('/blacklist list');
  await expect(player).toHaveReceivedMessage('No dimensions are blacklisted', { since: emptied });
});

test('an ordinary player cannot set a home in a blacklisted dimension', async ({ player, createPlayer, server }) => {
  await player.makeOp();
  player.chat('/blacklist add world_nether');
  await expect(player).toHaveReceivedMessage('world_nether has been added to the blacklist');

  try {
    const ordinary = await createPlayer();
    await standIn(server, ordinary, NETHER);

    ordinary.chat('/create-home nether-home');
    await expect(ordinary).toHaveReceivedMessage('You cannot set a home in this dimension because it has been blacklisted');
  } finally {
    player.chat('/blacklist remove world_nether');
    await expect(player).toHaveReceivedMessage('world_nether has been removed from the blacklist');
  }
});

test('an op holding bypass-blacklist may set a home there anyway', async ({ player, server }) => {
  await player.makeOp();
  player.chat('/blacklist add world_nether');
  await expect(player).toHaveReceivedMessage('world_nether has been added to the blacklist');

  try {
    await standIn(server, player, NETHER);
    player.chat('/create-home nether-home');
    await expect(player).toHaveReceivedMessage('nether-home has been created successfully');
  } finally {
    player.chat('/blacklist remove world_nether');
    await expect(player).toHaveReceivedMessage('world_nether has been removed from the blacklist');
  }
});

test('a name that is not a world is rejected', async ({ player }) => {
  await player.makeOp();

  player.chat('/blacklist add not_a_world');
  await expect(player).toHaveReceivedMessage('is not a valid world');
});
