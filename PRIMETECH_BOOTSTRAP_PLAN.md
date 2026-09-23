# TerminalP Bootstrap Plan

## Confirmed current behavior

The app embeds the upstream Termux `bootstrap-<arch>.zip` archives from the `termux-packages` release:

`https://github.com/termux/termux-packages/releases/download/bootstrap-2026.02.12-r1%2Bapt.android-7/bootstrap-<arch>.zip`

Gradle downloads them into `app/src/main/cpp/`, converts each archive into a native blob, and packages it into the APK. `TermuxInstaller` extracts archive entries into a staging directory below the app's `files` directory, creates entries listed in `SYMLINKS.txt`, then renames staging to `$PREFIX`.

The extractor currently sets mode `0700` for files under `bin/`, `libexec`, and selected apt paths. That is sufficient for executability and is not a relocation mechanism.

## Confirmed relocation-sensitive data

The archive contains `bin/login` as a shell script whose first line is:

`#!/data/data/com.termux/files/usr/bin/sh`

`SYMLINKS.txt` also contains absolute source paths under `/data/data/com.termux/files/usr`, including Termux keyring files. These are not safe when installed under TerminalP's prefix. Other scripts and package metadata may contain the same upstream prefix. ELF files can additionally contain compiled-in runpaths, interpreter or loader metadata, and library paths that cannot safely be changed by byte replacement.

## Immediate login failure

The reported `exec("/data/data/com.primetech.terminal/files/usr/bin/login"): Permission denied` is consistent with the archive's `bin/login` being a script whose shebang points to the nonexistent official Termux shell. Java's `canExecute()` only checks the extracted file's mode; it does not validate the shebang target. The extractor does set `0700` for `bin/login`, so missing execute permission is not the primary explanation. Device-side `ls -l`, `readlink`, and `logcat` should be captured to distinguish a bad shebang from SELinux or filesystem policy, but the archive inspection already proves the bootstrap is not PrimeTech-native.

## Native bootstrap requirements

Build from `termux-packages` using its bootstrap generation workflow (`scripts/generate-bootstraps.sh`) and configure the build's package prefix variables for the TerminalP runtime prefix. The resulting packages must be compiled/processed with:

- `$PREFIX=/data/data/com.primetech.terminal/files/usr`
- `$HOME=/data/data/com.primetech.terminal/files/home` supplied by the app environment
- `$PATH=$PREFIX/bin`
- dynamic linker/runpaths and native library lookup compatible with the app prefix
- shell scripts and shebangs generated for the selected prefix
- `SYMLINKS.txt` containing only valid TerminalP or relative targets
- apt/dpkg configuration and repository metadata appropriate to the selected PrimeTech package repository

The package build system already uses `TERMUX_PREFIX`/`@TERMUX_PREFIX@` substitutions and is the correct place to make this change. Do not binary-search-and-replace archives.

## What may initially remain upstream-derived

Source code, licenses, package recipes, architecture-specific Android compatibility patches, and the terminal UI can remain upstream-derived. The package repository can initially remain Termux-hosted only if its packages were built for the PrimeTech prefix and the product explicitly accepts that dependency.

## What must become PrimeTech-specific

The embedded bootstrap artifacts, absolute paths, shebangs, symlinks, ELF runpaths/interpreter assumptions, environment file, package-manager configuration, repository identity, and update/migration metadata must be PrimeTech-specific before claiming an independent userspace.

## Migration/update strategy

Use a fresh-prefix installation for the first native bootstrap. Existing prefixes must not be silently rewritten. Future updates should verify an archive manifest and version, install into a staging prefix, run validation, and atomically replace the old prefix with a documented rollback path. Package database and user home data should be kept separate so the runtime can be rebuilt without deleting user files.
