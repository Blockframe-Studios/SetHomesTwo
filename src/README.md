# Developer guide

How to work on the plugin. The root `README.md` is the user-facing
documentation (commands, permissions, example config, changelog) and is what
server owners read. Keep the two in step, but do not merge them: they have
different readers.

This file is the procedure: what to install, what to run, and how a change gets
from a branch into a release.

## Prerequisites

- **JDK 21.** The pom compiles with `<release>21</release>`.
- **Maven 3.9.6 or newer.** Maven builds the only artifact.
- Nothing else. The end-to-end suite needs Node, but Gradle downloads its own
  copy on a workstation and CI installs one, so you never run `npm` by hand.

## Build

    mvn package

Produces the shaded jar in `target/`. `mvn verify` does the same and also
exercises the shade relocation, which is what CI runs.

## Running it on a server

The sibling `../Spigot/` directory is a local Paper test server. Launch it with
`../Spigot/start.bat`, which copies the newest `SetHomesTwo-*.jar` out of
`target/` into `plugins/` before starting. So `mvn package` then `start.bat` is
the whole loop. It opens a JDWP debug port on 5005 for a remote debugger.

Plugin runtime data (config.yml, the SQLite database) lives in
`../Spigot/plugins/SetHomesTwo/`.

Paper remaps the jar on first load into `../Spigot/plugins/.paper-remapped/`.
Delete that cache when a freshly built jar appears not to take effect.

## Tests

Two suites, and both have to pass before a pull request merges.

### Unit suite (MockBukkit, no server)

    mvn test

389 tests as of 2026-08-22, about 17 seconds. A single class runs with
`-Dtest=HomesDaoTest`, several with commas rather than plus signs:
`-Dtest=A,B`.

Read the `Skipped: 0` line, not just `BUILD SUCCESS`. MockBukkit's
`UnimplementedOperationException` extends JUnit's `TestAbortedException`, so
reaching an unimplemented mock method is reported as a skip rather than a
failure and the build still passes. A test can quietly stop testing anything.
`support/FailOnUnimplemented`, registered on `ServerTestBase`, turns those
aborts into real failures. Do not remove it.

### End-to-end suite (a real Paper server and real bots)

    mvn -B clean package -DskipTests
    ./gradlew plugwrightTest

Boots Paper 1.21.11, installs the jar you just built, and joins bot players
that run commands and click through the GUI. 39 tests, about twenty seconds
once the server is up.

Maven has to run first. Gradle stages the newest jar out of `target/` and never
builds one itself: Gradle is the end-to-end test runner and nothing else.

The wrapper pins Gradle 8.10.2, which supports Java 23 at the highest. If your
default JDK is newer than that, point `JAVA_HOME` at your JDK 21, or every
Gradle command fails with an error that does not name the real cause.

`./gradlew plugwrightRunServer` boots the same server with the same jar and
config and leaves it running, so you can join `localhost` and drive the plugin
by hand. Specs live in `src/test/e2e` and are TypeScript. The server config
they assume is written by `build.gradle.kts`, so change a value there rather
than in a spec. Logs from the last run are in `run/logs/`, and the next run
wipes them.

That config grants `sh2.player` to everyone and leaves the bypass nodes on
`sh2.admin`, which gives the specs two tiers to test against. A bot that calls
`makeOp()` skips the teleport delay and the max-homes cap; one that does not
takes both. Tests that need to stand still through the countdown disable the
bot's physics first, because the move check compares locations exactly and an
idle bot still sends position packets.

CI runs the unit suite on every push and pull request, and the end-to-end
suite on every pull request.

## Branching

Two long-lived branches. `master` is the release branch, `dev` is the
integration branch, and `dev` is the default base.

Branch off `dev` for everything: features, refactors, tests, docs, chores. The
one exception is an immediate fix for a live bug bad enough to ship a release
on its own, which branches off `master` and then has to be merged into `dev` as
well, or the next release re-introduces the bug. When it is not obvious which
applies, ask. The default is `dev`.

**Name the branch `issue-<number>-<slug>`.** The first push of a branch named
that way moves the issue to In progress on the project board. A branch named
anything else moves nothing, and nothing warns you.

## Pull requests

Open it against `dev` unless the rule above put you on `master`.

**Name every issue it resolves with a closing keyword**, `Closes #54` or
`Fixes #54`. GitHub does not link an issue to a pull request merging into
`dev`, so that text is the only record, and it drives the board through In
review, Ready for release, and closed. A bare `#54` is a mention and closes
nothing, which is the right way to reference an issue in passing.

Rebase merging must stay disabled on the repository. The release step recovers
pull request numbers from commit subjects, which exist only in the merge form
(`Merge pull request #NN from ...`) and the squash form (`(#NN)`). A rebase
merge leaves neither, so the release would find no pull requests and close
nothing, silently.

Pushing to `master` ships a release: a version bump, a jar on BukkitDev, and an
update prompt for every server owner. That is why work pools on `dev`, and why
promoting `dev` to `master` is done by hand by the maintainers.

## Changesets

Every pull request that should change the released version adds one file to
`.changeset/`. A pull request without one releases nothing, which is what you
want for docs, tests, CI and refactors. The gate that enforces this fails
closed, so anything it does not recognize as unshippable is treated as needing
a changeset.

    git changeset

Prompts for the bump and a one-line summary, then writes and stages the file.
Skip the prompts with `git changeset patch "Summary of the change"`.

One-time setup per clone for the alias, or just run `bash scripts/changeset.sh`:

    git config --local include.path ../.gitconfig

Write the summary for server owners, not for reviewers. It is published as
written in three places: the README changelog, the GitHub Release notes and the
BukkitDev changelog. Keep the wording plain, because the text passes through
shell variables and JSON on its way to CurseForge.

### Choosing the bump

**Always `patch`.** The script offers all three, but a changeset in this
project never predicts a larger bump. Minor and major are a decision the
maintainers make by hand when `dev` is promoted to `master`.

For reference, the levels mean:

- `patch`: a bug fix, no behavior change for existing setups
- `minor`: new functionality, existing setups keep working
- `major`: breaking, in that a command, permission, config key or runtime
  requirement changes for existing servers

### What happens on merge

Merging to `master` bumps `pom.xml`, adds the summaries to the README
changelog, runs the tests, tags, and publishes to GitHub Releases and
BukkitDev. Every file in `.changeset/` is consumed, and the highest bump
present wins.

A failed release cannot be re-run: the changesets are already consumed, so a
re-run reports nothing to release. Finish by hand from the pushed tag. Keep
`pom.xml` off `-SNAPSHOT` between releases; the workflow rejects it.
