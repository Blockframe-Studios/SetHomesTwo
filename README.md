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
- **Permissions you can change from the config.** Every `sh2.*` node has a sensible default, and any of them can be moved in `config.yml`. No permissions plugin required.

## Quick start

1. Drop the jar into your `plugins` folder and restart the server.
2. Run `/sethome base` where you are standing.
3. Run `/homes` and click it.

That is genuinely the whole setup. Since 1.1.0 the player-facing permissions default to granted, so your players can create and use homes the moment the plugin loads. If you want to take one of those away, or hand an admin command to a non-operator, you can do it from `config.yml`. A permissions plugin is only needed for per-rank home limits.

## Managing homes

![The per-home management menu](docs/img/manage-menu.png)

Open your homes with `/homes`, or right-click the homes item. Then:

| Action | What happens |
| --- | --- |
| Left-click a home | Teleports you there |
| Right-click a home | Opens the management menu below |
| Rename | Opens an anvil prompt for the new name |
| Move home here | Repoints the home at where you are standing |
| Set icon to held item | The home's icon becomes whatever you are holding |
| Delete | Asks for confirmation first |

![Right-clicking a home to rename it](docs/img/rename.gif)

Home names are unique per player and ignore case, so `base` and `Base` are the same home. Management is controlled by `sh2.manage-homes`, which defaults to granted.

Changing a home's icon works the same way. Hold the item you want and click **Set icon to held item**:

![Changing a home's icon to the item being held](docs/img/change-icon.gif)

## Teleporting

![The stand-still countdown before a teleport](docs/img/teleport-delay.gif)

By default players wait three seconds before a teleport fires, and moving cancels it, so a home is not a free escape from a fight. Set `delay: 0` for instant teleports, or `cancelOnMove: false` to let players walk during the countdown.

![Instant teleport](docs/img/teleport-instant.gif)

Before it drops anyone anywhere, Set Homes Two checks the destination is safe to stand in. If a home has been built over, flooded with lava, or left hanging above a drop, the player is moved to the nearest safe spot instead, or the teleport is cancelled and they are told why. Turn it off with `teleportSafety: false`.

## Coming from EssentialsX or Set Homes v1

Your players keep their homes. The old plugin does not even need to be running, because the importer reads its data files directly.

- Run `/import-homes essentialsx` (or `/import-homes sethomes`). This is a **preview only**. It reports how many homes it would import and warns about any it would skip, and changes nothing.
- Happy with the numbers? Run it again with `confirm` on the end.
- Existing homes are never overwritten, so re-running it is always safe. Homes in worlds that no longer exist are skipped with a warning naming the world.

Afterwards, remove the old jar. Set Homes Two provides `/sethome`, `/home` and `/delhome`, and two plugins claiming the same commands will fight over them.

<details>
<summary>Set Homes v1: what each command and permission became</summary>

| Set Homes v1 | Set Homes Two |
| --- | --- |
| `/sethome [name] [description]` | `/sethome [name] [icon] [description]` |
| `/home [name]` | `/home [name]` |
| `/homes [player]` | `/list-homes` for your own list, `/get-player-homes <player>` for someone else's. `/homes` now opens the menu instead. |
| `/delhome [name]` | `/delhome <name>` |
| `/uhome <name> [description]` | `/uhome <name>` |
| `/home-of <player> [home]` | `/home-of <player> <home>` |
| `/delhome-of <player> [home]` | `/delhome-of <player> <home>` |
| `/uhome-of <player> [home]` | `/uhome-of <player> <home>` |
| `/blacklist <add\|remove> <world>` | `/blacklist <add\|remove\|list> <world...>` |
| `/setmax <group> <number>` | `/set-max-homes <group> <number>` |

| v1 permission | Set Homes Two permission |
| --- | --- |
| `homes.home` | `sh2.go-home`, plus `sh2.teleport` to actually arrive |
| `homes.sethome` | `sh2.create-home` |
| `homes.delhome` | `sh2.delete-home` |
| `homes.gethomes` | `sh2.get-player-homes` |
| `homes.home-of` | `sh2.go-player-home` |
| `homes.delhome-of` | `sh2.delete-player-home` |
| `homes.uhome` | `sh2.move-home` |
| `homes.uhome-of` | `sh2.move-player-home` |
| `homes.blacklist_add` | `sh2.add-to-blacklist` |
| `homes.blacklist_remove` | `sh2.remove-from-blacklist` |
| `homes.blacklist_list` | `sh2.get-blacklisted-dimensions` |
| `homes.setmax` | `sh2.set-max-homes` |
| `homes.config_bypass` | `sh2.bypass-max-homes`, `sh2.bypass-blacklist` and `sh2.bypass-teleport-delay` |
| `homes.strike` | Nothing |
| `homes.*` | `sh2.admin` |

Worth knowing before you copy a permissions file across:

- **`homes.config_bypass` is three nodes now.** In v1 it let a player exceed the home limit, set homes in blacklisted worlds, and skip the teleport delay and cooldown, all at once. Grant all three `sh2.bypass-*` nodes to reproduce that. Nothing is lost on the cooldown, because Set Homes Two has no cooldown feature.
- **Your v1 unnamed home is called `default`.** The importer files it under that name, and a bare `/sethome` or `/home` uses the same name, so both keep working exactly as they did. `/home-of steve default` reaches an imported unnamed home.
- **`/sethome` takes a description straight after the name again**, as it did in v1. Set Homes Two adds an optional icon in between, so a second word naming a real item is read as the icon. `/sethome base d my main base` forces the default icon and keeps the whole phrase.
- **The one-letter aliases are not provided.** v1 registered `/h`, `/sh`, `/dh`, `/lh`, `/ho`, `/dho`, `/uh`, `/uho`, `/bl` and `/sm`. `/h` in particular collides with several other homes plugins, and Bukkit resolves a collision silently by prefixing one of them, which is worse than not having it. If you want them, map them yourself in the server's own `commands.yml`.
- **`/homes` means something different.** In v1 it printed a chat list. In Set Homes Two it opens the homes menu, and `/list-homes` prints the chat list.

</details>

## Commands

| Command | Long form | What it does |
| --- | --- | --- |
| `/sethome [name] [icon] [description]` | `/create-home` | Creates a home where you stand. With no name it is called `default`. |
| `/home [name]` | `/go-home` | Teleports you to a home. With no name it goes to `default`. |
| `/homes` | - | Opens the homes menu. |
| `/delhome <name>` | `/delete-home` | Deletes a home. |
| `/uhome <name>` | `/move-home` | Moves one of your homes to where you are standing. |
| `/list-homes` | - | Lists your homes in chat. Click a name to teleport. |
| `/give-homes-item` | - | Gives you the item that opens the menu. |

Home names ignore case, so `/home Base` and `/delhome Base` both find a home called `base`.

On `/sethome`, a second word that names a real item becomes the icon, and everything after it is the description. So `/sethome base stone house` creates `base` with a stone icon and the description "house". If you wanted the whole phrase as the description, put `d` in the icon position: `/sethome base d stone house`. The reply names the icon it chose, so there is never any guessing.

<details>
<summary>Admin commands</summary>

| Command | What it does |
| --- | --- |
| `/set-max-homes [group] <number>` (alias `/setmax`) | Sets the home limit, per LuckPerms group or server-wide. |
| `/get-player-homes <player>` | Lists another player's homes. |
| `/home-of <player> <home>` (long form `/go-player-home`) | Teleports you to another player's home. |
| `/delhome-of <player> <home>` (long form `/delete-player-home`) | Deletes another player's home. |
| `/uhome-of <player> <home>` (long form `/move-player-home`) | Moves another player's home to where you are standing. |
| `/blacklist add <world...>` (alias `/add-to-blacklist`) | Stops homes being set in a world. |
| `/blacklist remove <world...>` (alias `/remove-from-blacklist`) | Lifts the restriction again. |
| `/blacklist list` (alias `/get-blacklisted-dimensions`) | Shows which worlds are blacklisted. |
| `/import-homes <sethomes\|essentialsx> [confirm]` | Imports homes from another plugin. Dry-run unless `confirm` is given. |

The three blacklist commands are now one command with three aliases. Nothing you already type changes: `/add-to-blacklist world_nether` still adds that world, and `/get-blacklisted-dimensions` still lists them. Give worlds by the name the server knows them by, in lower case, which on a default setup means `world`, `world_nether` and `world_the_end`.

The three commands that take a player accept anyone who has saved homes, whether or not they are online. Tab completion only offers online players, because there is no lookup for every stored name. Both the player name and the home name ignore case.

</details>

## Permissions

Nothing here needs a permissions plugin. Every node has a default, and you can change any of those defaults from `config.yml`. See [Changing permissions](#changing-permissions) below.

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
| `sh2.move-home` | everyone | Moving your own home with `/uhome` |
| `sh2.set-max-homes` | OP | Setting home limits |
| `sh2.get-player-homes` | OP | Viewing another player's homes |
| `sh2.go-player-home` | OP | Teleporting to another player's home |
| `sh2.delete-player-home` | OP | Deleting another player's home |
| `sh2.move-player-home` | OP | Moving another player's home |
| `sh2.add-to-blacklist` | OP | Blacklisting a world |
| `sh2.remove-from-blacklist` | OP | Un-blacklisting a world |
| `sh2.get-blacklisted-dimensions` | OP | Listing blacklisted worlds |
| `sh2.import-homes` | OP | Importing from another plugin |
| `sh2.update-notify` | OP | Being told on join that a newer release exists |
| `sh2.bypass-max-homes` | OP | Creating homes past the configured maximum, whether the limit is server-wide or per group |
| `sh2.bypass-blacklist` | OP | Creating a home in a blacklisted world, moving a home into one, and teleporting to a home already in one. It also stops `/homes` and `/list-homes` replacing the home's description with "Cannot teleport here: dimension blacklisted" |
| `sh2.bypass-teleport-delay` | OP | Teleporting with no countdown, and not being cancelled by moving |

Two bundles group those nodes so you can grant a whole role at once:

| Bundle | Default | Contains |
| --- | --- | --- |
| `sh2.player` | everyone | `sh2.create-home`, `sh2.go-home`, `sh2.list-homes`, `sh2.delete-home`, `sh2.teleport`, `sh2.give-homes-item`, `sh2.manage-homes`, `sh2.move-home` |
| `sh2.admin` | OP | `sh2.player`, plus every OP node in the table above |

The bundles are what actually grant these nodes. Each individual node is declared off in `plugin.yml`, and `sh2.player` or `sh2.admin` switches its whole set on, which is why denying a bundle takes that whole set away in one line. Granting or denying an individual node still works exactly as the table describes.

Note that `sh2.move-home` sits in `sh2.player`, not behind `sh2.manage-homes`. If you took `sh2.manage-homes` away to stop players relocating their homes, deny `sh2.move-home` as well or `/uhome` gives the ability back.

</details>

### Changing permissions

You can change any node's default from `config.yml`, with no permissions plugin involved. Uncomment the `permissions:` block and list the nodes you want to move:

```yaml
permissions:
  sh2.manage-homes: false
  sh2.get-player-homes: true
  sh2.import-homes: op
```

Accepted values are `true` (everyone), `false` (nobody), `op` (operators only) and `not-op` (everyone except operators). Bukkit reads these, so case variants such as `OP` and spellings such as `notop` are accepted too, but stick to the four above. A value it cannot read is skipped with a warning in the server log, as is a node name that does not exist, and every override that does apply is written to the log at startup. There is no wildcard form, so list each node.

The two bundles are nodes in their own right, so `sh2.player: false` moves the whole player set at once and `sh2.admin: true` hands every admin command to everybody. That last one is rarely what you want.

**This only changes a default.** If you run LuckPerms or similar, an explicit grant or deny there still wins. The config block decides what happens to a player the permissions plugin says nothing about.

Take care with `sh2.import-homes`. `/import-homes <source> confirm` writes homes for every player on the server and there is no second check inside the command, so granting it to everyone is a real risk. The plugin logs a warning if you move it off `op`.

With LuckPerms, the equivalent one-liner is:

```
/lp group default permission set sh2.player true
```

If you would rather not touch `config.yml` at all, the server's own `permissions.yml` can wrap the nodes in a rank of your own:

```yaml
myserver.moderator:
  default: false
  children:
    sh2.player: true
    sh2.get-player-homes: true
    sh2.go-player-home: true
```

Then grant `myserver.moderator` to whoever should have it.

## Configuration

Settings live in **`plugins/SetHomesTwo/config.yml`** on your server, written the first time the plugin starts. Edit it in any text editor, save, then **restart the server**. There is no in-game reload command, so changes do not apply until the server comes back up.

The file is commented throughout, and every message the plugin sends can be rewritten in it. These are the settings most servers actually change:

| Setting | Default | What it does |
| --- | --- | --- |
| `delay` | `3` | Seconds you must stand still before teleporting. `0` is instant. |
| `cancelOnMove` | `true` | Cancel the teleport if the player moves during the countdown. |
| `teleportSafety` | `true` | Relocate to the nearest safe spot instead of teleporting into danger. |
| `maxHomeEnabled` | `false` | Turn home limits on. |
| `maxHomesType` | `groups` | `singular` for one server-wide limit, `groups` for per-rank limits. |
| `openHomeItem` | `compass` | The item players right-click to open the menu. |
| `defaultHomeItem` | `white_wool` | Icon a home gets when the player names none. |
| `inventoryTitle` | `Your homes` | Title of the homes menu. |
| `maxHomeNameLength` | `32` | Longest home name allowed. |
| `permissions` | commented out | Changes the default of any `sh2.*` node. See [Changing permissions](#changing-permissions). |

Per-rank limits need [LuckPerms](https://luckperms.net/download) and `maxHomesType: groups`.

That table is only the common settings. For the complete list, see [`default-config.yml`](https://github.com/Blockframe-Studios/SetHomesTwo/blob/master/src/main/resources/default-config.yml), the file your `config.yml` is first written from. Every setting the plugin has is in there, commented in place.

<details>
<summary>Upgrading? Your existing config.yml will not gain the new settings</summary>

Set Homes Two never touches a `config.yml` that already exists, so settings added in a later release do not appear in a file written by an earlier one. Any missing setting quietly falls back to its default, so nothing breaks, but you cannot change a setting you cannot see.

To pick one up, copy the key you want out of [`default-config.yml`](https://github.com/Blockframe-Studios/SetHomesTwo/blob/master/src/main/resources/default-config.yml) into your file and restart. To start clean, rename your `config.yml` and restart. A fresh one is written with everything in it, and you can copy your old values across.

</details>

## FAQ

<details>
<summary>How do my players teleport home?</summary>

Three ways, all equivalent: `/home <name>`, opening `/homes` and left-clicking, or right-clicking the assigned "Home Item" from `/give-homes-item`.

</details>

<details>
<summary>Only OPs can create homes. How do I let everyone in?</summary>

Update to 1.1.0 or later. On older versions every permission defaulted to OP; they now default to granted for players. If you use a permissions plugin that denies unlisted nodes, grant `sh2.player`, which covers every ordinary player node in one go.

</details>

<details>
<summary>How do I turn a permission off without installing a permissions plugin?</summary>

Uncomment the `permissions:` block in `config.yml` and set the node to `false`, `op` or `not-op`. See [Changing permissions](#changing-permissions).

</details>

<details>
<summary>A player says one of their homes shows "Cannot teleport here: dimension blacklisted". Why?</summary>

The world that home is in has been blacklisted, so the home is listed but not reachable. Check `/blacklist list`. Either take the world off the list with `/blacklist remove <world>`, or move the home somewhere else with `/uhome-of <player> <home>` while standing where it should go.

</details>

<details>
<summary>How do I give donors more homes than everyone else?</summary>

Install LuckPerms, set `maxHomeEnabled: true` and `maxHomesType: groups`, then run `/set-max-homes <group> <number>` for each rank.

</details>

<details>
<summary>Can I run it alongside EssentialsX?</summary>

Not comfortably. Both register `/sethome`, `/home` and `/delhome`, and whichever loads last wins. Import your homes, then remove EssentialsX.

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

Found a bug or want a feature? Open an issue on [GitHub](https://github.com/Blockframe-Studios/SetHomesTwo/issues). It gets seen faster than a comment on this page.

[Source](https://github.com/Blockframe-Studios/SetHomesTwo) | [Report a bug](https://github.com/Blockframe-Studios/SetHomesTwo/issues) | [Donate](https://www.paypal.com/donate/?business=8LXCRFX27B37C&no_recurring=0&item_name=Thanks+for+your+support.+It+helps+keep+this+plugin+up+to+date+%3A%29&currency_code=USD)

## Changelog

#### 1.2.2 (2026-08-15)

- Fixed the update notice repeating on every join. An available release is now announced once and then held back for `updateReminderDays` (7 by default) before it is mentioned again; a newer release is still announced straight away. Set `updateReminderDays: 0` to announce each release exactly once.

#### 1.2.1 (2026-08-14)

- Added an update notice: the console at startup, and admins holding `sh2.update-notify` as they join, are told when a newer release is on GitHub. Set `checkForUpdates: false` to stop the plugin making the request at all.

#### 1.2.0 (2026-08-12)

Behaviour changes to be aware of before updating:
- Home names are now unique per player without regard to case, so you can no longer create both `base` and `Base`. Homes you already have are untouched, including any existing duplicates.
- `/delete-home` now deletes a single home. Previously it deleted every home whose name matched, so a player with duplicate names lost all of them at once.
- The plugin now requires Java 21 to run (previously Java 9). Minecraft 1.21 servers already require Java 21, so most setups need no change.

- Added a home management menu: right-click a home in the homes GUI to rename it, move it to where you are standing, set its icon to the item you are holding, or delete it. Deleting asks for confirmation first, and renaming opens an anvil prompt for the new name.
- Added a "Right click to edit home" hint to homes in the list, shown only to players who are able to manage them.
- Added the `sh2.manage-homes` permission, which controls the management menu and defaults to granted.
- Added config keys for the management menu: `manageHomeTitle`, `renamePromptTitle`, `manageHomeHint`, `maxHomeNameLength`, the button item and name pairs, and the new success and error messages. See `default-config.yml`.
- Fixed home updates and deletes not being scoped to the owning player.
- Fixed the homes list hiding a home behind the previous-page button once a player had 46 or more homes, and showing an empty second page to a player with exactly 45 homes.

#### 1.1.0 (2026-08-11)
- Added `/sethome`, `/delhome`, and `/home` as classic aliases for `/create-home`, `/delete-home`, and `/go-home`.
- Player permissions (create-home, go-home, list-homes, delete-home, teleport, give-homes-item) now default to granted for all players; admin permissions still default to OP.
- Added the `/homes` command, which opens the homes GUI directly; `/list-homes` still prints the chat listing.
- Added `/import-homes` to import homes from Set Homes v1 or EssentialsX (dry-run by default, pass `confirm` to apply).
- Added a teleport safety check that relocates players to the nearest safe spot, or cancels the teleport, when a home would put them in blocks, lava, or a dangerous fall.
- Teleport destination chunks are now loaded during the countdown, so arriving at a distant home is smoother.
- Fixed stale teleport attempts surviving a server restart, which could block a player's next teleport (finishes #14).
- Fixed error when maxHomesType is groups and LuckPerms is not installed. The limit is now skipped with a console warning instead.

#### Earlier releases
- Added go-home and list-homes commands.
- Fixed issue where players missing sh2.teleport could not break blocks.
- Fixed issue where a player who has an open homes gui has their inventory overwritten by the next person to open a homes gui.
