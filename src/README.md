# Changesets

Every pull request that should change the released version adds one file here.
A pull request without one releases nothing - which is what you want for docs,
tests, CI and refactors.

    git changeset

Prompts for the bump and a one-line summary, then writes and stages the file.
Skip the prompts with `git changeset minor "Summary of the change"`.

One-time setup per clone for the alias, or just run `bash scripts/changeset.sh`:

    git config --local include.path ../.gitconfig

## Choosing the bump

- `patch` - a bug fix, no behaviour change for existing setups
- `minor` - new functionality, existing setups keep working
- `major` - breaking: a command, permission, config key or runtime
  requirement changes for existing servers

## What happens on merge

Merging to `master` bumps `pom.xml`, adds the summaries to the README
changelog, runs the tests, tags, and publishes to GitHub Releases and
BukkitDev. Every file here is consumed: the highest bump present wins.

A failed release cannot be re-run - the changesets are already consumed, so a
re-run reports nothing to release. Finish by hand from the pushed tag. Keep
`pom.xml` off `-SNAPSHOT` between releases; the workflow rejects it.
