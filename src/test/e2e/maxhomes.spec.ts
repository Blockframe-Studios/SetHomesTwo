import { test, expect } from '@drownek/plugwright';

// The harness config sets maxHomesType singular with a cap of 3.
const CAP = 3;

test('an ordinary player is capped at the configured maximum', async ({ player }) => {
  for (let i = 0; i < CAP; i++) {
    player.chat(`/create-home home${i}`);
    await expect(player).toHaveReceivedMessage(`home${i} has been created successfully`);
  }

  const since = player.getMessageBufferIndex();
  player.chat('/create-home one-too-many');
  await expect(player).toHaveReceivedMessage('You have reached the maximum number of homes allowed', { since });
});

test('an op holding bypass-max-homes may exceed the cap', async ({ player }) => {
  await player.makeOp();

  for (let i = 0; i <= CAP; i++) {
    player.chat(`/create-home home${i}`);
    await expect(player).toHaveReceivedMessage(`home${i} has been created successfully`);
  }
});

test('deleting a home frees a slot', async ({ player }) => {
  for (let i = 0; i < CAP; i++) {
    player.chat(`/create-home home${i}`);
    await expect(player).toHaveReceivedMessage(`home${i} has been created successfully`);
  }

  player.chat('/delete-home home0');
  await expect(player).toHaveReceivedMessage('home0 has been deleted successfully');

  const since = player.getMessageBufferIndex();
  player.chat('/create-home replacement');
  await expect(player).toHaveReceivedMessage('replacement has been created successfully', { since });
});
