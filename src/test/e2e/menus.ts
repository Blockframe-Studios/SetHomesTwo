import { expect, type PlayerWrapper } from '@drownek/plugwright';

/**
 * Opens a home's management menu.
 *
 * The locator API clicks with the left button only and the menu is bound to a
 * right click, so the slot goes to mineflayer directly. Button 1 is the right
 * button, mode 0 an ordinary click.
 */
export async function openActions(player: PlayerWrapper, name: string) {
  player.chat('/homes');
  const homes = await player.gui({ title: 'E2E homes' });
  await expect.poll(() => homes.locator(i => i.getDisplayName().includes(name)).displayName()).toContain(name);

  const home = player.getCurrentGui()!.items.find(i => i.getDisplayName().includes(name));
  await player.bot.clickWindow(home!.slot, 1, 0);

  return player.gui({ title: `Manage: ${name}` });
}
