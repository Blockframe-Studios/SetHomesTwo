import { test, expect, sleep } from '@drownek/plugwright';
import { openActions } from './menus.js';

test('right-clicking a home opens its management menu', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  const actions = await openActions(player, 'base');
  expect(actions.title).toContain('Manage: base');
  await expect.poll(() => actions.locator(i => i.getDisplayName().includes('Rename')).displayName()).toContain('Rename');
});

test('move home here relocates the home', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  const actions = await openActions(player, 'base');
  await actions.locator(i => i.getDisplayName().includes('Move home here')).click();

  await expect(player).toHaveReceivedMessage('base has been moved to your current location');
});

test('set icon to held item changes the icon', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.giveItem('diamond');
  await expect(player).toContainItem('diamond');
  const diamond = player.bot.inventory.items().find(item => item.name === 'diamond');
  await player.bot.equip(diamond!, 'hand');

  const actions = await openActions(player, 'base');
  await actions.locator(i => i.getDisplayName().includes('Set icon to held item')).click();

  await expect(player).toHaveReceivedMessage('The icon for base is now');
});

test('delete asks for confirmation first', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  const actions = await openActions(player, 'base');
  await actions.locator(i => i.getDisplayName().includes('Delete')).click();

  await expect.poll(() => actions.locator(i => i.getDisplayName().includes('Confirm delete')).displayName()).toContain('Confirm delete');

  const since = player.getMessageBufferIndex();
  player.chat('/list-homes');
  await expect(player).toHaveReceivedMessage('base', { since });
});

test('confirming the delete removes the home', async ({ player }) => {
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  const actions = await openActions(player, 'base');
  await actions.locator(i => i.getDisplayName().includes('Delete')).click();
  await expect.poll(() => actions.locator(i => i.getDisplayName().includes('Confirm delete')).displayName()).toContain('Confirm delete');

  await actions.locator(i => i.getDisplayName().includes('Confirm delete')).click();
  await expect(player).toHaveReceivedMessage('base has been deleted successfully');

  await sleep(250);
  const since = player.getMessageBufferIndex();
  player.chat('/list-homes');
  await expect(player).toHaveReceivedMessage('You have not created any homes yet', { since });
});
