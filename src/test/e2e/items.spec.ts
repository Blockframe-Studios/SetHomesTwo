import { test, expect } from '@drownek/plugwright';

test('give-homes-item puts the homes compass in the inventory', async ({ player }) => {
  player.chat('/give-homes-item');
  await expect(player).toContainItem('compass');
});

test('right-clicking the compass opens the homes menu', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  player.chat('/give-homes-item');
  await expect(player).toContainItem('compass');

  const compass = player.bot.inventory.items().find(item => item.name === 'compass');
  await player.bot.equip(compass!, 'hand');
  player.bot.activateItem();

  const gui = await player.gui({ title: 'E2E homes' });
  await expect.poll(() => gui.locator(i => i.getDisplayName().includes('base')).displayName()).toContain('base');
});
