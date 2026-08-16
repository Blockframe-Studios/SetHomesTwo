---
bump: patch
---

Fixed the world blacklist only ever being enforced on the first three worlds of a server. Blacklisting any world beyond those reported success and then did nothing. It is enforced from this release on, so if you have blacklisted more than three worlds, run /blacklist list before you update. Homes in a world that starts being enforced stop being reachable by players who do not hold sh2.bypass-blacklist.
