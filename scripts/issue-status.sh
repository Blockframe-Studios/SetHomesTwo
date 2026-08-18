#!/usr/bin/env bash
# Parses issue and pull request references out of text.
#
# Pure text handling, no network, so the workflows that call it stay thin and
# the parsing is unit tested.

# GitHub's own closing keyword set. A bare #NN must not close anything: pull
# request bodies here routinely mention issues they do not resolve.
CLOSING_KEYWORDS='close|closes|closed|fix|fixes|fixed|resolve|resolves|resolved'

# Reads text on stdin, writes the issue numbers it closes to stdout, one per
# line, first appearance order, deduplicated. Always exits 0.
closing_refs() {
  grep -oiE "\\b(${CLOSING_KEYWORDS})[[:space:]]*:?[[:space:]]+#[0-9]+" \
    | grep -oE '[0-9]+' \
    | grep -E '^[1-9][0-9]*$' \
    | awk '!seen[$0]++'
  return 0
}

# Reads `git log --format=%s` output on stdin and writes the pull request
# numbers it contains to stdout, one per line, deduplicated. Subjects only:
# a body may mention a pull request the commit did not come from.
pr_numbers_from_log() {
  grep -oE '(Merge pull request #[0-9]+|\(#[0-9]+\))' \
    | grep -oE '[0-9]+' \
    | grep -E '^[1-9][0-9]*$' \
    | awk '!seen[$0]++'
  return 0
}

# What a push should do, given `open-pr` when the branch already has an open
# pull request and anything else when it does not. Prints the target status
# first, then the states it may advance from, one per line.
#
# A branch under review must not be reported as work in progress: merging the
# base branch in is a push like any other. Neither plan names Ready for release
# or Done, so a push can never pull a shipped issue backwards.
push_plan() {
  if [ "${1:-}" = "open-pr" ]; then
    printf 'In review\nTodo\nunset\nIn progress\n'
  else
    printf 'In progress\nTodo\nunset\n'
  fi
}
