---
bump: patch
---

Permission defaults can now be changed from config.yml with no permissions plugin installed. Uncomment the permissions block and set any sh2 node to true, false, op or not-op. Two bundles, sh2.player and sh2.admin, move a whole role at once, and three new bypass permissions were added for admins: sh2.bypass-max-homes, sh2.bypass-blacklist and sh2.bypass-teleport-delay. Operators hold those three by default, the same way Set Homes v1 granted homes.config_bypass through homes.*, so an operator is not held to the teleport delay, the world blacklist or the home limit. Set any of them to false in the permissions block if you would rather they were.
