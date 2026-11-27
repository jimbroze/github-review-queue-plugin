#!/usr/bin/env bash
set -euo pipefail

jq -c '
  .[]
  | select(.type == "result")
  | .permission_denials[]?
  | [.tool_name, (.tool_input | .command // .file_path // "" | .[0:300])]
' "$1"
