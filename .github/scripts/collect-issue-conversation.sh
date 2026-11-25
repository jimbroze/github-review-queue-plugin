#!/usr/bin/env bash
# Prints the issue as JSON, with only the comments written by the owner or the Claude app.
# Fails if the owner didn't write the issue, or if comment TRIGGER_COMMENT_ID (optional) is missing.
set -euo pipefail

issue_file=$(mktemp)
gh api "repos/$GH_REPO/issues/$ISSUE_NUMBER" > "$issue_file"

if [ "$(jq -r .user.login "$issue_file")" != "$GITHUB_REPOSITORY_OWNER" ]; then
  gh issue comment "$ISSUE_NUMBER" --body "Claude only works on issues written by @$GITHUB_REPOSITORY_OWNER, because other authors can edit an issue after it is approved. Copy this into a new issue of your own and label that \`ai\`." >&2
  exit 1
fi

gh api --paginate --slurp "repos/$GH_REPO/issues/$ISSUE_NUMBER/comments?per_page=100" |
  jq --slurpfile issue "$issue_file" --arg owner "$GITHUB_REPOSITORY_OWNER" --arg trigger "${TRIGGER_COMMENT_ID:-}" '
    def from_owner: .user.login == $owner and .user.type == "User";
    def from_claude: .user.login == "claude[bot]" and .user.type == "Bot";
    (add // [])
    | map(select(from_owner or from_claude) | {id, author: .user.login, created_at, body})
    | if $trigger == "" or any(.id == ($trigger | tonumber)) then . else error("comment \($trigger) is missing") end
    | {title: $issue[0].title, body: $issue[0].body, comments: .}
  '
