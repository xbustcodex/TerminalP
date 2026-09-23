# TerminalP package-build audit

Scope: upstream sources in the sibling `termux-packages` checkout were inspected read-only. No files in that checkout were modified.

## Central identity and paths

| Variable/file | Upstream/current behavior | TerminalP action | ABI impact |
|---|---|---|---|
| `scripts/properties.sh: TERMUX_APP__PACKAGE_NAME` | Defaults to `com.termux`; `TERMUX_APP_PACKAGE` is a compatibility alias | Must be overridden centrally to `com.primetech.terminal` for a fork build | No CPU ABI change; changes embedded absolute paths and package identity |
| `TERMUX_APP__DATA_DIR` | Derived as `/data/data/$TERMUX_APP__PACKAGE_NAME` | Derives `/data/data/com.primetech.terminal` | No ABI change; runtime path change |
| `TERMUX__ROOTFS` / `TERMUX_BASE_DIR` | Derived as `$TERMUX_APP__DATA_DIR/files` | Derives `/data/data/com.primetech.terminal/files` | No ABI change |
| `TERMUX__HOME` | Derived as `$TERMUX__ROOTFS/home` | Derives `/data/data/com.primetech.terminal/files/home` | No ABI change |
| `TERMUX__PREFIX` / `TERMUX_PREFIX` | Derived as `$TERMUX__ROOTFS/usr` | Derives `/data/data/com.primetech.terminal/files/usr` | No ABI change, but native binaries/scripts may embed path |
| `TERMUX__PREFIX_CLASSICAL` | Retains original prefix for package/build logic | Must derive from PrimeTech prefix | No ABI change |
| `scripts/build/termux_step_setup_variables.sh` | Exports `prefix` and `PREFIX` from `TERMUX_PREFIX` | Automatically follows central override | No ABI change |

The properties file validates safe app-data, rootfs, home, and prefix paths. A controlled fork configuration should override the package name before derived variables are evaluated, rather than hardcoding hundreds of package recipes.

## Shebang and linker handling

`scripts/build/termux_step_massage.sh` scans package files and rewrites shell shebangs to `$TERMUX_PREFIX/bin/<interpreter>` unless already valid or excluded. Therefore a source build with the PrimeTech prefix produces PrimeTech shebangs.

The toolchain scripts (`scripts/build/toolchain/termux_setup_toolchain_29.sh` and `_23c.sh`) add `$TERMUX__PREFIX__LIB_DIR` to linker search/runpath flags. Android 7+ uses DT_RUNPATH. Package-specific build scripts can add their own RPATH/RUNPATH, so generated ELF files must still be audited.

## Bootstrap generation

There are two relevant upstream scripts:

- `scripts/generate-bootstraps.sh`: downloads already-built packages from the public Termux repository. It is unsuitable for a PrimeTech-native bootstrap because those packages were built using the upstream identity.
- `scripts/build-bootstraps.sh`: builds packages from local recipes and assembles a bootstrap. Its own help explicitly states that the package name/prefix are defined by `scripts/properties.sh`, and that a changed package name requires a forced rebuild.

The local-source script is the correct foundation. It includes apt, bash, dash, coreutils, termux-core, termux-exec, termux-keyring, termux-tools, and related dependencies. It creates dpkg metadata under `$TERMUX_PREFIX/var/lib/dpkg` and converts symlinks into `SYMLINKS.txt`.

## Package manager and repository

`generate-bootstraps.sh` defaults to `https://packages-cf.termux.dev/apt/termux-main` and embeds the selected repository in apt setup performed by package recipes/second-stage files. A PrimeTech bootstrap may use upstream-hosted repositories only as a temporary source if the packages were built for the same PrimeTech prefix and ABI. Existing upstream binary packages are not automatically compatible merely because the APK package ID changed.

The package name is also used to decide whether prebuilt dependency downloads may be reused (`TERMUX_REPO_APP__PACKAGE_NAME` is compared with `TERMUX_APP_PACKAGE`). A PrimeTech build should not reuse a repository built for `com.termux` without proving path compatibility.

## Maintainer scripts and metadata

Bootstrap assembly stores package lists, md5sums, control metadata, and maintainer scripts under `$TERMUX_PREFIX/var/lib/dpkg`. The second-stage bootstrap script substitutes the central `TERMUX_PREFIX`, package manager, architecture, and app name. This means central identity configuration propagates to dpkg paths and generated startup scripts.

## Required deviation

The maintainable change is a dedicated PrimeTech configuration/patch layer consumed by a private copy or controlled invocation of `termux-packages`, setting `TERMUX_APP__PACKAGE_NAME` before `properties.sh` derives dependent paths. Do not change upstream recipes globally and do not use the downloaded release archives. The generated artifacts must be checksum-pinned and embedded by TerminalP's Gradle build.
