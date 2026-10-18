# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Review Queue: a JetBrains IDE plugin (targets WebStorm, PhpStorm and IntelliJ IDEA; built against 2026.2.3, `sinceBuild` 262) with a tool window listing GitHub pull requests waiting on the user's review across all repositories. Re-review requests come first, then longest-waiting. Clicking a row opens the PR in the IDE's GitHub PR view where possible, falling back to the browser.

## Commands

```bash
./gradlew test                                   # unit tests (JUnit 4 + IntelliJ Platform test framework)
./gradlew test --tests '*ReviewQueueTest'        # one test class
./gradlew test --tests '*ReviewQueueTest.someName'  # one test
./gradlew verifyPlugin                           # compatibility check: WebStorm/PhpStorm/IDEA 2026.2.3, latest WebStorm EAP, local EAP if webStormEapPath is set
./gradlew buildPlugin                            # zip in build/distributions/, installable via "Install Plugin from Disk"
./gradlew runIde | runWebStorm | runPhpStorm | runWebStormEap   # sandbox IDE
./gradlew integrationTest -Pe2eIde=2026.2.3      # end-to-end tests (Starter/Driver, real IDE, Java 25)
```

- Unit tests use JDK 21; the `integrationTest` source set compiles and runs on JDK 25.
- End-to-end tests skip unless the `REVIEW_QUEUE_E2E_*` environment variables are set (see `src/integrationTest/.../E2eSettings.kt` and `.github/workflows/e2e-nightly.yml`, which runs them nightly against 2026.2.3 and EAP).
- Gradle build cache is on. If tests fail with `NoSuchMethodError` after a rebase, rerun with `--rerun-tasks`.

## Architecture

Packages under `src/main/kotlin/com/jimbroze/reviewqueue/`, from pure logic outward:

| Package | Role |
|---|---|
| `queue` | `ReviewRequest`, `ReviewQueue` ordering (`SortOrder`), `QueueFilter`. Pure Kotlin. |
| `timeline` | Turns a PR's timeline events into a `PendingReview` (when the wait started, re-review or not). Pure. |
| `worktime` | `WorkHours` / `ReviewTiming`: waiting time counted in working hours, overdue thresholds. Pure. |
| `github` | Fetches review requests via GitHub GraphQL (`GitHubReviewSource`, `HttpGitHubGraphQl`) using the IDE's GitHub account (`IdeAccountReviewRequests`). |
| `clones` | Finds local clones of a GitHub repo by scanning code folders and matching git remotes. Pure (filesystem only). |
| `ide` | Opening a PR inside the IDE: current project → another open window → local clone/clone popup → browser (`IdeFirstPullRequestOpener`, `OutsideOpenProjectsOpener`, wired in `IdePullRequestOpeners.kt`). |
| `toolwindow` | UI: `SharedReviewQueue` (app service, one queue for all windows, fetches only while some tool window is visible), `ReviewQueueService` (per-project view), `ReviewQueueViewModel` → `ReviewQueueState` → `ReviewQueuePanel`/`ReviewRowRenderer`. |
| `settings` | App-level settings page (work hours, sort, filters, auto-refresh), broadcast via `ReviewQueueSettingsListener.TOPIC`. |

Key constraints:

- **Internal GitHub-plugin API is confined to `ide/GitHubPluginPullRequestViewer.kt`.** It drives the PR tool window's repo selector and waits a bounded time, failing safe to the browser. Signatures differ between 2026.2 and 2026.3 (avoid `activateAndAwaitProject`). Keep all `org.jetbrains.plugins.github` internals in that file.
- IDE-facing classes take their collaborators as constructor parameters (functions/interfaces) so logic is unit-tested with in-memory fakes; the IDE wiring lives in small factory files.
- Time in tests is deterministic: pass a `Clock` and tick `Flow`s (`ticksEvery`), never real delays.
- UI strings live in `src/main/resources/messages/ReviewQueueBundle.properties`.
- Avoid internal platform APIs flagged by `verifyPlugin` (e.g. use `JBUI.emptyInsets()`, not `JBInsets.emptyInsets()`).
