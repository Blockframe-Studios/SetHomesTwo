import { test, expect } from '@drownek/plugwright';

// Paper's Brigadier tree hides a node the sender fails the permission check
// for, so a denied command reads as unknown, not as the Bukkit denial.
const DENIED = 'Unknown or incomplete command';

test('an ordinary player may manage their own homes', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  const since = player.getMessageBufferIndex();
  player.chat('/list-homes');
  await expect(player).toHaveReceivedMessage('base', { since });
});

test('an ordinary player is refused an admin command', async ({ player }) => {
  player.chat('/blacklist list');
  await expect(player).toHaveReceivedMessage(DENIED);
});

test('an op may run the admin command', async ({ player }) => {
  await player.makeOp();

  player.chat('/blacklist list');
  await expect(player).toHaveReceivedMessage('No dimensions are blacklisted');
});

test('deOp takes the admin command away again', async ({ player }) => {
  await player.makeOp();
  player.chat('/blacklist list');
  await expect(player).toHaveReceivedMessage('No dimensions are blacklisted');

  await player.deOp();
  const since = player.getMessageBufferIndex();
  player.chat('/blacklist list');
  await expect(player).toHaveReceivedMessage(DENIED, { since });
});

test('a node pinned to op in the config is detached from the player bundle', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  // sh2.move-home is pinned to op by the harness config, which detaches it from
  // the sh2.player bundle this player otherwise holds in full.
  const since = player.getMessageBufferIndex();
  player.chat('/move-home base');
  await expect(player).toHaveReceivedMessage(DENIED, { since });

  await player.makeOp();
  const afterOp = player.getMessageBufferIndex();
  player.chat('/move-home base');
  await expect(player).toHaveReceivedMessage('has been moved to your current location', { since: afterOp });
});
