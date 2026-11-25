#!/usr/bin/env bash
# Prints the owner's feedback on a PR as JSON: conversation comments, review bodies and inline comments.
# Fails if comment TRIGGER_COMMENT_ID is missing.
set -euo pipefail

feedback_dir=$(mktemp -d)
gh api --paginate --slurp "repos/$GH_REPO/issues/$PR_NUMBER/comments?per_page=100" > "$feedback_dir/comments.json"
gh api --paginate --slurp "repos/$GH_REPO/pulls/$PR_NUMBER/reviews?per_page=100" > "$feedback_dir/reviews.json"
gh api --paginate --slurp "repos/$GH_REPO/pulls/$PR_NUMBER/comments?per_page=100" > "$feedback_dir/inline.json"

jq -n \
  --slurpfile comments "$feedback_dir/comments.json" \
  --slurpfile reviews "$feedback_dir/reviews.json" \
  --slurpfile inline "$feedback_dir/inline.json" \
  --arg owner "$GITHUB_REPOSITORY_OWNER" \
  --argjson trigger "$TRIGGER_COMMENT_ID" '
    def from_owner: add // [] | map(select(.user.login == $owner and .user.type == "User"));
    {
      comments: ($comments[0] | from_owner | map({id, created_at, body})),
      reviews: ($reviews[0] | from_owner | map(select(.body != "") | {submitted_at, body})),
      inline_comments: ($inline[0] | from_owner | map({created_at, path, line, diff_hunk, body}))
    }
    | if any(.comments[]; .id == $trigger) then . else error("comment \($trigger) is missing") end
  '
