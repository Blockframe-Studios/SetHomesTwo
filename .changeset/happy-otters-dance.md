---
bump: patch
---

Importing from Set Homes v1 no longer loses a home whose name differs only in capitalisation. Set Homes v1 allowed one player to hold both 'base' and 'Base', while home names here ignore case, so the second one is now imported under the next free name such as 'Base2' instead of being silently dropped. The import report and the server log both name it, and the preview now reports the same numbers as the confirm that follows it.
