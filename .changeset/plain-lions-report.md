---
bump: patch
---

/blacklist add and /blacklist remove no longer report success when the database write actually failed. A failure now says so and the world is left as it was. Permission overrides in config.yml are also matched without regard to the case you write the node in, so a line like SH2.manage-homes now applies rather than being logged as applied and quietly ignored.
