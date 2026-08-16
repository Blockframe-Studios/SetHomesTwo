---
bump: patch
---

Importing from Set Homes v1 or EssentialsX now resolves each player's name from the server's own player cache, so admin commands like /get-player-homes and /home-of work on a migrated player immediately instead of requiring them to log in first. A player the server has never seen still imports fine; they are picked up on their next join, same as before.
