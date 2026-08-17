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
