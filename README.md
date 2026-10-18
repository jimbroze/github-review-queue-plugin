# Review Queue

> **Note:** This plugin is mostly vibe-coded. It was built test-first (TDD), but not every test got a
> human review: some were reviewed, others were skipped.

A JetBrains IDE plugin that lists every GitHub pull request waiting on your review, across all
repositories, in one tool window.

- **Re-review requests first**, then the PRs that have waited longest.
- **Waiting time in working hours**, with overdue PRs highlighted.
- **Click a row** to open the PR in the IDE's GitHub pull request view. If the repo isn't open, it
  looks for a local clone (or offers to clone it), and falls back to the browser.
- **One shared queue** for all open project windows. It only refreshes while a Review Queue tool
  window is visible.

Settings (work hours, sort order, filters, auto-refresh) live under **Settings → Tools → Review
Queue**.

## Requirements

- WebStorm, PhpStorm or IntelliJ IDEA **2026.2** or later.
- A GitHub account added in the IDE (**Settings → Version Control → GitHub**). The plugin uses it
  to query GitHub.

## Install

```bash
./gradlew buildPlugin
```

Then in the IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the zip from
`build/distributions/`.

## Develop

```bash
./gradlew test             # unit tests (JDK 21)
./gradlew runIde           # sandbox IDEA; also runWebStorm, runPhpStorm
./gradlew verifyPlugin     # compatibility check against supported IDEs
./gradlew integrationTest -Pe2eIde=2026.2.3   # end-to-end tests in a real IDE (JDK 25)
```

End-to-end tests skip unless the `REVIEW_QUEUE_E2E_*` environment variables are set. See
[`CLAUDE.md`](CLAUDE.md) for the architecture overview.
