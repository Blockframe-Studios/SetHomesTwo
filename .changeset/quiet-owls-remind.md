---
bump: patch
---

Fixed the update notice repeating on every join. An available release is now announced once and then held back for `updateReminderDays` (7 by default) before it is mentioned again; a newer release is still announced straight away. Set `updateReminderDays: 0` to announce each release exactly once.
