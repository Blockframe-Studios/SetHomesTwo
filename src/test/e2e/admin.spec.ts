import { test, expect, waitUntil, sleep } from '@drownek/plugwright';

test("an admin can open another player's homes", async ({ player, createPlayer }) => {
  const other = await createPlayer();
  other.chat('/create-home theirs');
  await expect(other).toHaveReceivedMessage('theirs has been created successfully');

  await player.makeOp();
  player.chat(`/get-player-homes ${other.username}`);

  const gui = await player.gui({ title: `Homes of ${other.username}` });
  await expect.poll(() => gui.locator(i => i.getDisplayName().includes('theirs')).displayName()).toContain('theirs');
});

test("an admin can teleport to another player's home", async ({ player, createPlayer, server }) => {
  const other = await createPlayer();
  other.chat('/create-home theirs');
  await expect(other).toHaveReceivedMessage('theirs has been created successfully');

  await player.makeOp();
  await player.setGameMode('spectator');
  const target = other.bot.entity.position.clone();
  await player.teleport(target.x + 80, target.y, target.z + 80);
  await waitUntil(() => player.bot.entity.position.distanceTo(target) > 20);

  player.chat(`/go-player-home ${other.username} theirs`);
  await waitUntil(() => player.bot.entity.position.distanceTo(target) < 4, { timeout: 15000 });
  void server;
});

test("an admin can move another player's home", async ({ player, createPlayer }) => {
  const other = await createPlayer();
  other.chat('/create-home theirs');
  await expect(other).toHaveReceivedMessage('theirs has been created successfully');

  await player.makeOp();
  player.chat(`/move-player-home ${other.username} theirs`);
  await expect(player).toHaveReceivedMessage(`${other.username}'s home 'theirs' has been moved to your location`);
});

test("an admin can delete another player's home", async ({ player, createPlayer }) => {
  const other = await createPlayer();
  other.chat('/create-home theirs');
  await expect(other).toHaveReceivedMessage('theirs has been created successfully');

  await player.makeOp();
  player.chat(`/delete-player-home ${other.username} theirs`);
  await expect(player).toHaveReceivedMessage(`${other.username}'s home 'theirs' has been deleted`);

  const since = other.getMessageBufferIndex();
  other.chat('/list-homes');
  await expect(other).toHaveReceivedMessage('You have not created any homes yet', { since });
});

test("the admin view of another player's homes offers no management menu", async ({ player, createPlayer }) => {
  const other = await createPlayer();
  other.chat('/create-home theirs');
  await expect(other).toHaveReceivedMessage('theirs has been created successfully');

  await player.makeOp();
  player.chat(`/get-player-homes ${other.username}`);
  const gui = await player.gui({ title: `Homes of ${other.username}` });
  await expect.poll(() => gui.locator(i => i.getDisplayName().includes('theirs')).displayName()).toContain('theirs');

  const snapshot = player.getCurrentGui();
  const home = snapshot!.items.find(i => i.getDisplayName().includes('theirs'));
  await player.bot.clickWindow(home!.slot, 1, 0);

  await sleep(750);
  const title = player.getCurrentGui()?.title ?? '';
  expect(title.includes('Manage:')).toBe(false);
});
