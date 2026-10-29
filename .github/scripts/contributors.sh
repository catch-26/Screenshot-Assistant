#!/usr/bin/env bash
# Prints "Contributors: @a @b" for the commit authors and co-authors in
# $1..$2, so the GitHub release lists them in its Contributors section.
# Bots are left out. Prints nothing if there is no one to credit.
set -euo pipefail

from=$1
to=$2

{
  gh api "repos/$GITHUB_REPOSITORY/compare/$from...$to" --paginate --jq '.commits[].author.login // empty'
  git log --format='%(trailers:key=Co-authored-by,valueonly)' "$from..$to" \
    | grep -oiP '<\K[0-9]+\+[^@]+(?=@users\.noreply\.github\.com>)' \
    | sort -u \
    | while IFS=+ read -r id login; do
        current=$(gh api "user/$id" --jq .login 2>/dev/null || true)
        [[ $current =~ ^[A-Za-z0-9-]+$ ]] && echo "$current" || echo "$login"
      done || true
  git log --format='%(trailers:key=Co-authored-by,valueonly)' "$from..$to" \
    | grep -qi '<noreply@anthropic\.com>' && echo claude || true
} | grep -v '\[bot\]$' | awk 'NF && !seen[$0]++ {printf "%s@%s", (n++ ? " " : "Contributors: "), $0} END {if (n) print ""}'
