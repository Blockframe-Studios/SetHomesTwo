# Set Homes Two

### Introduction
This plugin allows players to set multiple homes, across various worlds, and then teleport to them with ease. 
Additionally, server admins have the ability to blacklist certain dimensions, restricting the ability to set a home in those dimensions. 
If a home has already been created in a dimension and that dimension gets blacklisted later, the player will still see the home in their inventory, but will not be able to teleport to it.
Using the config you can control settings such as: a maximum number of homes, teleport cool down, and teleport delay. As well, all the messages sent to the user are fully customizable.

### Installation
Simply place the downloaded jar into your server's "plugins" folder and launch the server.
A default config will be created on the first launch and then after that feel free to make any edits you like.
An example setup can also be found right before the screenshots section.

**NOTE**: For `maxHomes` to work you must install the soft dependency below, and setup groups for the respective permissions plugin.

### Migrating from Set Homes v1 or EssentialsX
Already have homes in Set Homes (v1) or EssentialsX? You can bring them over with one command. The old plugin does not need to be running - the importer reads its data files directly, so this works even if you already removed the old jar.

1. Make sure the old plugin's data folder is still present on your server: `plugins/SetHomes/homes.yml` for Set Homes v1, or `plugins/Essentials/userdata/` for EssentialsX.
2. Install Set Homes Two and start the server.
3. As an operator (or anyone with `sh2.import-homes`), run `/import-homes sethomes` or `/import-homes essentialsx` from chat or the console. This is a preview only - it reports how many homes would be imported and warns about any that would be skipped, without changing anything.
4. Run the same command again with `confirm` (for example `/import-homes sethomes confirm`) to apply.

Good to know:
- Existing homes are never overwritten. A home whose name is already taken for that player is skipped, which also means re-running the command is always safe.
- Homes in worlds that no longer exist on the server are skipped with a warning naming the world.
- Imported homes use your configured `defaultHomeItem` as their icon - players can recreate a home with `/create-home` if they want a custom icon.
- A v1 "default" home (set with plain `/sethome`) is imported under the name `default`.
- After migrating, remove the old plugin's jar if you have not already - Set Homes Two provides `/sethome`, `/home`, and `/delhome`, and two plugins registering the same commands will conflict.

### Soft Dependencies
- [LuckPerms](https://luckperms.net/download)

### Commands
- `/create-home [name] [display_material | d | default] [description]` `(alias: /sethome)` - Will create a home where the player is standing with the given name, material chosen, and description.
- `/go-home [name]` `(alias: /home)` - Will teleport the player to their home with the given name. Players will also need the sh2.teleport permission to use this command effectively.
- `/list-homes` - Prints a listing of the players created homes. A home can be teleported to by clicking on the underlined home name (requires player to have sh2.teleport and sh2.go-home permissions).
- `/homes` - Opens the homes menu directly.
- `/delete-home [name]` `(alias: /delhome)` - Will delete the home with the name provided.
- `/add-to-blacklist [dimension names]` - This will add the specified dimension to the blacklisted table. If a dimension is present in this table, players will not be able to have save their homes in that dimension.
- `/remove-from-blacklist [dimension names]` - This will remove the specified dimension from the blacklisted table.
- `/get-blacklisted-dimensions` - This will return a list of the dimensions that are in the blacklisted table
- `/set-max-homes [group name] [number]` - This will update the maximum number of homes that players are allowed to set. 
If the plugin is set up to allow different groupings or tiers for players, you will need to provide a group name in addition to the number of max homes. 
If the plugin is set up to only have one group or tier, you only need to provide the number of max homes.
- `/import-homes [sethomes|essentialsx] [confirm]` - Imports homes from Set Homes v1 or EssentialsX. Dry-run unless confirm is given.

### Managing homes from the GUI

Open your homes with `/homes` or by right-clicking the homes item, then:

- **Left-click** a home to teleport to it.
- **Right-click** a home to open its management menu, where you can rename it, move it to where you are standing, set its icon to the item you are holding, or delete it.

Deleting always asks for confirmation first. Renaming opens an anvil where you type the new name. Home names must be unique per player and are compared without regard to case, so you cannot have both `base` and `Base`.

Management is controlled by the `sh2.manage-homes` permission, which defaults to true.

### Permissions

As of 1.1.0, the player permissions below default to granted for all players; the admin permissions default to OP only.

- `sh2.give-homes-item` - Allow player to get homes viewing/teleportation item.
- `sh2.create-home` - Allow player to create homes.
- `sh2.go-home` - Allow player to execute go-home command.
- `sh2.list-homes` - Allow player to list their homes in chat window.
- `sh2.delete-home` - Allow player to delete their own homes.
- `sh2.teleport` - Allow player to teleport to created homes.
- `sh2.add-to-blacklist` - Add dimensions to blacklist table.
- `sh2.remove-from-blacklist` - Remove dimensions from blacklist table.
- `sh2.get-blacklisted-dimensions` - Retrieve a list of the blacklisted dimensions.
- `sh2.get-player-homes` - Retrieve a list of a given player's homes.
- `sh2.set-max-homes` - Set the max number of homes all players, or individual groups, can have.
- `sh2.import-homes` - Import homes from another homes plugin (Set Homes v1 or EssentialsX).
- `sh2.manage-homes` - Allow player to rename, move, re-icon, or delete their homes from the GUI.

### Extra Features
- The time it takes to teleport to a saved home can be configured
- Players can choose which Minecraft item they would like to use as an icon for their saved homes (each home can use a different Minecraft item)
- Server admins can configure what the default item is for saved home's icon

### Example Config
~~~yaml
# -- HOMES --
# The item the player will use when
# right-clicking to open the homes list.
openHomeItem: "compass"

# The default item when create-home command
# is only given a home name, or is only
# provided with the default material name.
defaultHomeItem: "white_wool"

# The title of the inventory displaying the
# players home list.
inventoryTitle: "Your homes"

# Enabled maximum number of homes.
maxHomeEnabled: false

# The maximum number of homes setup type.
# Choices are: singular | groups
# Example of singular maxHomesType:
# maxHomesType: "singular"
# maxHomes: 5
maxHomesType: groups
maxHomes:
  admin: 5
  user: 4

# -- TELEPORTING --
teleportTitle: "Please stand still"
teleportSubtitle: "You will be teleported in %d..." # You can use %d here as a placeholder for the seconds counter.
teleportSuccess: "Teleported to %s" # You can use %s here as a placeholder for the home name the player was teleported to.
cancelOnMove: true # true | false
delay: 3 # (seconds) 0 is no delay.
teleportSafety: true # Relocate to the nearest safe spot (or cancel) when a home would teleport you into blocks, lava, or a fall.

# -- MESSAGES --
homeCreated: "%s has been created successfully." # You can use %s here as a placeholder for the players home name.
homeDeleted: "%s has been deleted successfully." # You can use %s here as a placeholder for the players home name.
dimensionAddedToBlacklist: "%s has been added to the blacklist." # You can use %s here as a placeholder for the dimension names.

# -- ERROR MESSAGES --
invalidHomeItem: "The material you entered is not valid, please try a different one."
falseHomeItem: "This home item does not belong to you."
teleportedWhileTeleporting: "You cannot teleport while already teleporting."
movedWhileTeleporting: "Teleport has been cancelled because you have moved."
noHomes: "You have not created any homes yet. Use /create-home to make your first one."
teleportToBlacklistedDimension: "You cannot teleport to this home because the dimension is blacklisted."
maxHomesReached: "You have reached the maximum number of homes allowed."
dimensionBlacklisted: "You cannot set home in this dimension because it is blacklisted."
unsafeHome: "Teleport cancelled: this home is not safe to stand in and no safe spot was found nearby."
movedToSafeSpot: "Your home was not safe to stand in, so you were moved to the nearest safe spot."

# -- DEBUGGING --
debugLevel: "error" # Choices are: error | info

# -- HOME MANAGEMENT GUI --
# Title of the per-home management menu.
# %s is replaced with the home name.
manageHomeTitle: "Manage: %s"

# Title of the anvil prompt shown when renaming a home.
renamePromptTitle: "New home name"

# Hint shown on each home in the homes list, below the description.
# Only shown to players who can actually manage the home (sh2.manage-homes)
# and never on another player's homes. Supports '&' colour codes.
# Set to "" to hide it.
manageHomeHint: "&7Right click to edit home"

# Maximum number of characters allowed in a home name.
maxHomeNameLength: 32

# Buttons in the management menu.
# Button names support '&' colour codes (e.g. &c for red).
renameButtonItem: "name_tag"
renameButtonName: "Rename"
moveHomeButtonItem: "ender_pearl"
moveHomeButtonName: "Move home here"
setIconButtonItem: "item_frame"
setIconButtonName: "Set icon to held item"
deleteButtonItem: "barrier"
deleteButtonName: "&cDelete"
backButtonItem: "arrow"
backButtonName: "Back"
confirmButtonItem: "lime_wool"
confirmButtonName: "&aConfirm delete"
cancelButtonItem: "red_wool"
cancelButtonName: "&cCancel"

# -- HOME MANAGEMENT MESSAGES --
homeRenamed: "%s has been renamed to %s." # First %s is the old name, second is the new name.
homeMoved: "%s has been moved to your current location." # %s is the home name.
homeIconChanged: "The icon for %s is now %s." # First %s is the home name, second is the material.

# -- HOME MANAGEMENT ERROR MESSAGES --
duplicateHomeName: "You already have a home called '%s'." # %s is the duplicate name.
invalidHomeName: "That home name is not valid. Names must not be blank."
homeNameTooLong: "That home name is too long. The maximum is %d characters." # %d is the configured maximum.
homeNoLongerExists: "That home no longer exists."
emptyHandForIcon: "Hold the item you want to use as the icon, then click again."
cannotMoveToBlacklistedDimension: "You cannot move a home into this dimension because it has been blacklisted."
~~~

### Running tests
The plugin has an automated test suite built on MockBukkit, JUnit, and an in-memory SQLite database.

```
mvn test
```

This runs under MockBukkit and needs no Minecraft server. `mvn verify` additionally builds the shaded jar, which checks the AnvilGUI relocation.

### Donations
Please feel free to donate via the button below, any amount is greatly appreciated. Donating will help keep this plugin up to date. Thank you!

[![Donate](./src/main/img/donateBtn.png)](https://www.paypal.com/donate/?return=https://dev.bukkit.org/projects/312833&cn=Add+special+instructions+to+the+addon+author()&business=sam%40samleighton.us&bn=PP-DonationsBF:btn_donateCC_LG.gif:NonHosted&cancel_return=https://dev.bukkit.org/projects/312833&lc=US&item_name=Set+Homes+(from+bukkit.org)&cmd=_donations&rm=1&no_shipping=1&currency_code=USD)

### Screenshots
![Screenshot](https://imgur.com/ucK48vf.png)
![Screenshot](https://imgur.com/xoifjIv.png)
![Screenshot](https://imgur.com/TnqcR9i.png)
![Screenshot](https://imgur.com/pR3qJ2Q.png)

### F.A.Q
- **Q: How can I give players permission to set named homes?**
  **A:** You will need to install the permission plugin, [LuckPerms](https://luckperms.net/download) then configure the config.yml to allow for multiple groups (see above for example config).

### Releasing

Releases are automated. A pull request that should change the version adds a
changeset:

    git changeset

**One-time setup per clone**, to get the `git changeset` command:

    git config --local include.path ../.gitconfig

Git will not read a config file out of a working tree on its own - that would
let any repository you clone run commands you never agreed to - so this opt-in
is deliberate. Skip it and use `bash scripts/changeset.sh` instead; the two are
the same script.

It asks whether the change is a patch, minor or major and for a one-line
summary, then writes a file under `.changeset/`.

When a pull request carrying a changeset merges to `master`, the release
workflow computes the next version from every changeset present, updates
`pom.xml` and the changelog below, runs the full test suite, tags the commit,
and publishes to GitHub Releases and BukkitDev. A pull request with no
changeset releases nothing.

The workflow cannot resume a release it already started. Once the version
commit and tag are pushed to `master`, the changesets that drove them are
gone, so re-running the workflow just reports nothing to release - it will
not retry the part that failed. If the BukkitDev upload or the GitHub
Release step fails after that point, finish it by hand: build the jar at
the pushed tag and upload it to whichever destination did not complete.
BukkitDev is uploaded before the GitHub Release is created, deliberately,
so a failure there is caught before anything goes public. Also keep
`pom.xml` off a `-SNAPSHOT` version between releases - the workflow's
version computation rejects it outright, and the release fails at the
planning step before anything else runs.

### Changelog

#### 1.2.0 (2026-08-12)

Behaviour changes to be aware of before updating:
- Home names are now unique per player without regard to case, so you can no longer create both `base` and `Base`. Homes you already have are untouched, including any existing duplicates.
- `/delete-home` now deletes a single home. Previously it deleted every home whose name matched, so a player with duplicate names lost all of them at once.
- The plugin now requires Java 21 to run (previously Java 9). Minecraft 1.21 servers already require Java 21, so most setups need no change.

- Added a home management menu: right-click a home in the homes GUI to rename it, move it to where you are standing, set its icon to the item you are holding, or delete it. Deleting asks for confirmation first, and renaming opens an anvil prompt for the new name.
- Added a "Right click to edit home" hint to homes in the list, shown only to players who are able to manage them.
- Added the `sh2.manage-homes` permission, which controls the management menu and defaults to granted.
- Added config keys for the management menu: `manageHomeTitle`, `renamePromptTitle`, `manageHomeHint`, `maxHomeNameLength`, the button item and name pairs, and the new success and error messages. See the example config above.
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
