![Set Homes Two](docs/img/logo.png)

**Set Homes Two gives every player a menu of their homes.** Left-click one to teleport. Right-click it to rename it, move it, change its icon, or delete it.

[Source](https://github.com/Blockframe-Studios/SetHomesTwo) | [Report a bug](https://github.com/Blockframe-Studios/SetHomesTwo/issues) | [Donate](https://www.paypal.com/donate/?business=8LXCRFX27B37C&no_recurring=0&item_name=Thanks+for+your+support.+It+helps+keep+this+plugin+up+to+date+%3A%29&currency_code=USD)

![The homes menu, with each home shown as its own item](docs/img/homes-menu.png)

## Why Set Homes Two

- **A menu, or a list of commands.** Homes live in a chest-style GUI. Players open it with `/homes` or by right-clicking the configured "Home Item".
- **Every home gets its own icon.** Pick any Minecraft item when you create a home, or change it later to whatever you are holding. A base, a mine and a farm stop looking identical.
- **Rename, move and delete in-game.** Right-click any home to manage it. Deleting always asks first, so nobody loses a base to a misclick.
- **Teleports that do not kill you.** Set Homes Two checks the destination and relocates you to the nearest safe spot rather than dropping you into blocks, lava, or a fall.
- **Switch without losing anything.** One command imports every home from EssentialsX or Set Homes v1, and shows you exactly what it will do before it does it.
- **Per-rank home limits.** Give donors more homes than default players with LuckPerms groups, or set one server-wide limit.

## Quick start

1. Drop the jar into your `plugins` folder and restart the server.
2. Run `/sethome base` where you are standing.
3. Run `/homes` and click it.

That is genuinely the whole setup. Since 1.1.0 the player-facing permissions default to granted, so your players can create and use homes the moment the plugin loads. You only need a permissions plugin if you want per-rank home limits.

## Managing homes

![The per-home management menu](docs/img/manage-menu.png)

Open your homes with `/homes`, or right-click the homes item. Then:

| Action | What happens |
| --- | --- |
| Left-click a home | Teleports you there |
| Right-click a home | Opens the management menu below |
| Rename | Opens an anvil prompt - type the new name |
| Move home here | Repoints the home at where you are standing |
| Set icon to held item | The home's icon becomes whatever you are holding |
| Delete | Asks for confirmation first |

![Right-clicking a home to rename it](docs/img/rename.gif)

Home names are unique per player and ignore case, so `base` and `Base` are the same home. Management is controlled by `sh2.manage-homes`, which defaults to granted.

Changing a home's icon works the same way - hold the item you want and click **Set icon to held item**:

![Changing a home's icon to the item being held](docs/img/change-icon.gif)

## Teleporting

![The stand-still countdown before a teleport](docs/img/teleport-delay.gif)

By default players wait three seconds before a teleport fires, and moving cancels it - so a home is not a free escape from a fight. Set `delay: 0` for instant teleports, or `cancelOnMove: false` to let players walk during the countdown.

![Instant teleport](docs/img/teleport-instant.gif)

Before it drops anyone anywhere, Set Homes Two checks the destination is safe to stand in. If a home has been built over, flooded with lava, or left hanging above a drop, the player is moved to the nearest safe spot instead, or the teleport is cancelled and they are told why. Turn it off with `teleportSafety: false`.

## Coming from EssentialsX or Set Homes v1

Your players keep their homes. The old plugin does not even need to be running - the importer reads its data files directly.

- Run `/import-homes essentialsx` (or `/import-homes sethomes`). This is a **preview only**. It reports how many homes it would import and warns about any it would skip, and changes nothing.
- Happy with the numbers? Run it again with `confirm` on the end.
- Existing homes are never overwritten, so re-running it is always safe. Homes in worlds that no longer exist are skipped with a warning naming the world.

Afterwards, remove the old jar. Set Homes Two provides `/sethome`, `/home` and `/delhome`, and two plugins claiming the same commands will fight over them.

## Commands

| Command | Long form | What it does |
| --- | --- | --- |
| `/sethome <name> [item] [description]` | `/create-home` | Creates a home where you stand. The optional item becomes its icon. |
| `/home <name>` | `/go-home` | Teleports you to a home. |
| `/homes` | - | Opens the homes menu. |
| `/delhome <name>` | `/delete-home` | Deletes a home. |
| `/list-homes` | - | Lists your homes in chat. Click a name to teleport. |
| `/give-homes-item` | - | Gives you the item that opens the menu. |

<details>
<summary>Admin commands</summary>

| Command | What it does |
| --- | --- |
| `/set-max-homes [group] <number>` | Sets the home limit, per LuckPerms group or server-wide. |
| `/get-player-homes <player>` | Lists another player's homes. |
| `/add-to-blacklist <dimension...>` | Stops homes being set in a dimension. |
| `/remove-from-blacklist <dimension...>` | Lifts the restriction again. |
| `/get-blacklisted-dimensions` | Shows which dimensions are blacklisted. |
| `/import-homes <sethomes\|essentialsx> [confirm]` | Imports homes from another plugin. Dry-run unless `confirm` is given. |

</details>

## Permissions

<details>
<summary>Full permission list</summary>

| Permission | Default | Allows |
| --- | --- | --- |
| `sh2.create-home` | everyone | Creating homes |
| `sh2.go-home` | everyone | Using the go-home command |
| `sh2.teleport` | everyone | Actually teleporting to a home |
| `sh2.list-homes` | everyone | Listing homes in chat, and `/homes` |
| `sh2.delete-home` | everyone | Deleting your own homes |
| `sh2.give-homes-item` | everyone | Getting the menu item |
| `sh2.manage-homes` | everyone | Renaming, moving, re-iconing and deleting from the GUI |
| `sh2.set-max-homes` | OP | Setting home limits |
| `sh2.get-player-homes` | OP | Viewing another player's homes |
| `sh2.add-to-blacklist` | OP | Blacklisting a dimension |
| `sh2.remove-from-blacklist` | OP | Un-blacklisting a dimension |
| `sh2.get-blacklisted-dimensions` | OP | Listing blacklisted dimensions |
| `sh2.import-homes` | OP | Importing from another plugin |

</details>

## Configuration

Settings live in **`plugins/SetHomesTwo/config.yml`** on your server, written the first time the plugin starts. Edit it in any text editor, save, then **restart the server** - there is no in-game reload command, so changes do not apply until the server comes back up.

The file is commented throughout, and every message the plugin sends can be rewritten in it. These are the settings most servers actually change:

| Setting | Default | What it does |
| --- | --- | --- |
| `delay` | `3` | Seconds you must stand still before teleporting. `0` is instant. |
| `cancelOnMove` | `true` | Cancel the teleport if the player moves during the countdown. |
| `teleportSafety` | `true` | Relocate to the nearest safe spot instead of teleporting into danger. |
| `maxHomeEnabled` | `false` | Turn home limits on. |
| `maxHomesType` | `groups` | `singular` for one server-wide limit, `groups` for per-rank limits. |
| `openHomeItem` | `compass` | The item players right-click to open the menu. |
| `defaultHomeItem` | `white_wool` | Icon used when a home is created without one. |
| `inventoryTitle` | `Your homes` | Title of the homes menu. |
| `maxHomeNameLength` | `32` | Longest home name allowed. |

Per-rank limits need [LuckPerms](https://luckperms.net/download) and `maxHomesType: groups`. The [full annotated config](https://github.com/Blockframe-Studios/SetHomesTwo#example-config) is on GitHub.

<details>
<summary>Upgrading? Your existing config.yml will not gain the new settings</summary>

Set Homes Two never touches a `config.yml` that already exists, so settings added in a later release do not appear in a file written by an earlier one. Any missing setting quietly falls back to its default, so nothing breaks - but you cannot change a setting you cannot see.

To pick one up, copy the key you want out of the [full config](https://github.com/Blockframe-Studios/SetHomesTwo#example-config) into your file and restart. To start clean, rename your `config.yml` and restart - a fresh one is written with everything in it, and you can copy your old values across.

</details>

## FAQ

<details>
<summary>How do my players teleport home?</summary>

Three ways, all equivalent: `/home <name>`, opening `/homes` and left-clicking, or right-clicking the assigned "Home Item" from `/give-homes-item`.

</details>

<details>
<summary>Only OPs can create homes. How do I let everyone in?</summary>

Update to 1.1.0 or later. On older versions every permission defaulted to OP; they now default to granted for players. If you use a permissions plugin that denies unlisted nodes, grant `sh2.create-home`, `sh2.go-home` and `sh2.teleport`.

</details>

<details>
<summary>How do I give donors more homes than everyone else?</summary>

Install LuckPerms, set `maxHomeEnabled: true` and `maxHomesType: groups`, then run `/set-max-homes <group> <number>` for each rank.

</details>

<details>
<summary>Can I run it alongside EssentialsX?</summary>

Not comfortably - both register `/sethome`, `/home` and `/delhome`, and whichever loads last wins. Import your homes, then remove EssentialsX.

</details>

<details>
<summary>Where are homes stored?</summary>

In a SQLite database in `plugins/SetHomesTwo/`. Nothing external to install and nothing to configure.

</details>

## Requirements

- Paper or Spigot **1.21+**
- **Java 21**, which Minecraft 1.21 servers already require
- Optional: [LuckPerms](https://luckperms.net/download), only for per-rank home limits

## Support

Found a bug or want a feature? Open an issue on [GitHub](https://github.com/Blockframe-Studios/SetHomesTwo/issues) - it gets seen faster than a comment on this page.

[Source](https://github.com/Blockframe-Studios/SetHomesTwo) | [Report a bug](https://github.com/Blockframe-Studios/SetHomesTwo/issues) | [Donate](https://www.paypal.com/donate/?business=8LXCRFX27B37C&no_recurring=0&item_name=Thanks+for+your+support.+It+helps+keep+this+plugin+up+to+date+%3A%29&currency_code=USD)
