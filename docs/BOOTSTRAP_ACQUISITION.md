# Bootstrap acquisition

A fresh clone of TerminalP **cannot** build until the validated PrimeTech ARM64
bootstrap archive is present at `app/src/main/cpp/bootstrap-aarch64.zip`. The
archive is intentionally excluded from Git history (`.gitignore` matches
`*.zip`) because it is a ~167 MB binary build input, not source.

Acquisition is therefore an **explicit, reproducible, separate step** — never
something a normal Gradle build does silently.

## Mechanism

The archive is published as a **GitHub Release asset** on the TerminalP
repository itself, and fetched by a standalone script that verifies SHA-256
before writing anything to the build-input path.

```
scripts/fetch-bootstrap.sh
        ↓
  download release asset
        ↓
  verify SHA-256 (+ size)      ← fail closed on mismatch/truncation/HTTP error
        ↓
  atomically place at app/src/main/cpp/bootstrap-aarch64.zip
        ↓
  gradlew.bat :app:assembleDebug
        ↓
  app/build.gradle re-validates the same SHA-256  ← unchanged, still authoritative
```

## Artifact identity

| Field | Value |
|---|---|
| Source repository | `xbustcodex/TerminalP` |
| Release tag | `bootstrap-aarch64-2026.02.12-r1` |
| Release title | PrimeTech validated ARM64 bootstrap 2026.02.12-r1 |
| Asset name | `bootstrap-aarch64.zip` |
| Expected size | `167495746` bytes |
| **SHA-256** | `97d5bbb3e2db080e076e52966294197624ab7c4bf0ce0437f909a5ee57da83ba` |
| Destination | `app/src/main/cpp/bootstrap-aarch64.zip` |
| Download URL | `https://github.com/xbustcodex/TerminalP/releases/download/bootstrap-aarch64-2026.02.12-r1/bootstrap-aarch64.zip` |

These values are recorded in `scripts/bootstrap-aarch64.manifest`, which is the
single source of truth read by the fetch script. The same SHA-256 is pinned
independently in `app/build.gradle`.

## Commands

From Git Bash (Windows) or any POSIX shell:

```sh
# acquire (downloads, verifies, places the build input)
scripts/fetch-bootstrap.sh

# check an already-present build input without touching the network
scripts/fetch-bootstrap.sh --verify-only

# then build
./gradlew :app:assembleDebug          # Git Bash / Linux
gradlew.bat :app:assembleDebug        # Windows cmd / PowerShell
```

## Integrity and failure behaviour

`fetch-bootstrap.sh` **fails closed** — non-zero exit, no build input written —
on any of:

- SHA-256 mismatch of the downloaded artifact
- size mismatch (truncated/short transfer)
- HTTP failure (404, 403, 5xx), unreachable host, or unavailable artifact
- an existing file at the destination that does not match the expected digest
- a manifest SHA-256 that is not also pinned in `app/build.gradle`

It will **never**:

- silently substitute a different bootstrap
- fall back to an upstream Termux bootstrap
- regenerate the bootstrap during an Android build

The file is downloaded to a temporary path in the destination directory and
only `mv`-ed into place after verification, so a partial download can never
appear as a valid build input.

`app/build.gradle` continues to validate the SHA-256 at build time and still
throws `GradleException` on a missing or wrong-digest PrimeTech archive. That
validation was **not** weakened; the fetch script is an additional, earlier
gate.

## Authentication

No credential is stored in this repository. If the repository is private, the
script obtains a token at run time from, in order:

1. `GITHUB_TOKEN`
2. `GH_TOKEN`
3. `gh auth token`
4. the configured Git credential helper (e.g. Git Credential Manager)

If none is available it attempts anonymous access, which succeeds once the
repository is public.

## Publishing a new bootstrap

1. Build the bootstrap with the PrimeTech package builder.
2. Confirm its SHA-256.
3. Update `EXPECTED_SHA256` (and size) in `scripts/bootstrap-aarch64.manifest`
   **and** the matching pin in `app/build.gradle` — the fetch script refuses to
   run if they disagree.
4. Create a release tag named `bootstrap-aarch64-<version>` on
   `xbustcodex/TerminalP` and upload `bootstrap-aarch64.zip` as the asset.

Do not commit the archive itself.
