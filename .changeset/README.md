# Changesets

Every pull request that should change the released version adds one file here.

Create it with:

    python3 scripts/changeset.py

It asks for the kind of change and a one-line summary, then writes the file
and stages it. Pass arguments to skip the prompts:

    python3 scripts/changeset.py minor "Notify operators when a newer release is available"

A pull request with no changeset releases nothing, which is what you want for
documentation, tests, CI and refactors. Holding a changeset back is also how
you batch several merges into one release.

At release time every file here is consumed: the highest bump present decides
the version, the summaries become the changelog entries, and the files are
deleted.

## Choosing the bump

- `patch` - a bug fix, no behaviour change for existing setups
- `minor` - new functionality, existing setups keep working
- `major` - breaking: a command, permission, config key or runtime
  requirement changes for existing servers

Release 1.2.0 raised the required Java version from 9 to 21. That is a `major`
by this definition, and the kind of change this file exists to make deliberate.
