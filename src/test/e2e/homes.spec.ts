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

test('the homes menu lists the home', async ({ player }) => {
  await player.makeOp();

  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  player.chat('/homes');
  const gui = await player.gui({ title: 'E2E homes' });
  expect(gui.title).toContain('E2E homes');

  const home = gui.locator(i => i.getDisplayName().includes('base'));
  await expect.poll(() => home.displayName()).toContain('base');
});

test('clicking a home in the menu teleports the player', async ({ player }) => {
  await player.makeOp();
  await player.setGameMode('spectator');

  const origin = player.bot.entity.position.clone();
  player.chat('/create-home base');
  await expect(player).toHaveReceivedMessage('base has been created successfully');

  await player.teleport(origin.x + 60, origin.y, origin.z + 60);
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) > 20);

  player.chat('/homes');
  const gui = await player.gui({ title: 'E2E homes' });
  await gui.locator(i => i.getDisplayName().includes('base')).click();

  await expect(player).toHaveReceivedMessage('Teleported to base');
  await waitUntil(() => player.bot.entity.position.distanceTo(origin) < 2);
});

test('a player without sh2.create-home is refused and creates nothing', async ({ player }) => {
  player.chat('/create-home base');
  // Paper's Brigadier command tree hides a node a sender fails the permission
  // requirement for, so a denied command reads as unknown rather than the
  // classic Bukkit permission message.
  await expect(player).toHaveReceivedMessage('Unknown or incomplete command');

  // Opping afterwards is the only way to read the home list back, since
  // list-homes is gated too.
  await player.makeOp();
  player.chat('/list-homes');
  await expect(player).toHaveReceivedMessage('You have not created any homes yet');
});
