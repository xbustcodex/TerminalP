# PrimeTech Terminal Identity Audit

Scope: `D:\workSpace\termux-app` only.

## Canonical identity

| Item | PrimeTech value | Status |
|---|---|---|
| Android application ID | `com.primetech.terminal` | Set in `app/build.gradle` |
| Display label | `PrimeTech Terminal` | Set through Gradle manifest placeholder and resources |
| Android namespace | `com.termux` | **SAFE INTERNAL NAMESPACE**; intentionally unchanged |
| Private data root | `/data/data/com.primetech.terminal` | Derived by `TermuxConstants.TERMUX_PACKAGE_NAME` |
| Files root | `/data/data/com.primetech.terminal/files` | Derived constant |
| `$PREFIX` | `/data/data/com.primetech.terminal/files/usr` | Derived constant/resource |
| `$HOME` | `/data/data/com.primetech.terminal/files/home` | Derived constant |
| Provider authorities | `${TERMUX_PACKAGE_NAME}.documents` and `${TERMUX_PACKAGE_NAME}.files` | Resolve to PrimeTech identity |
| RUN_COMMAND permission/action | `${TERMUX_PACKAGE_NAME}.permission.RUN_COMMAND` and `${TERMUX_PACKAGE_NAME}.RUN_COMMAND` | Resolve to PrimeTech identity |
| Launcher shortcuts | `com.primetech.terminal` target package | Updated |

## Classification rules and findings

### SAFE INTERNAL NAMESPACE — leave unchanged

The Java/Kotlin source packages and library namespaces remain `com.termux.*`, including:

- `com.termux.app.*`
- `com.termux.shared.*`
- `com.termux.terminal.*`
- `com.termux.view.*`
- XML class names referencing those namespaces
- Java imports and resource IDs such as `com.termux.R`

These are implementation namespaces, not Android installed-application identity. Renaming them would be a broad, risky source migration and is explicitly out of scope.

### MUST CHANGE FOR PRIMETECH — changed

- `app/build.gradle`: `applicationId` is `com.primetech.terminal`.
- Gradle `TERMUX_PACKAGE_NAME` placeholder is `com.primetech.terminal`.
- Gradle app label is `PrimeTech Terminal`.
- App and shared resource entities use PrimeTech package/path values.
- `TermuxConstants.TERMUX_PACKAGE_NAME` and `TERMUX_APP_NAME` use PrimeTech values.
- Shortcut `targetPackage` values use `com.primetech.terminal`.
- Manifest authorities and custom permission/action values are placeholder-derived, so they resolve to PrimeTech values at build time.

### BOOTSTRAP/USERSPACE DEPENDENCY — requires follow-up

- `app/build.gradle` downloads official Termux bootstrap archives from the Termux packages release URL.
- Bootstrap archives are compiled artifacts and may contain `/data/data/com.termux` assumptions in binaries, scripts, ELF interpreter/linker paths, or package metadata.
- No bootstrap archive is currently checked into `app/src/main/cpp`; Gradle downloads them during the build.
- Changing Java constants does not rewrite those compiled assumptions.
- A PrimeTech-native bootstrap must be built from the package source with the PrimeTech prefix configuration; binary search/replace is unsafe.

### COMPANION APP DEPENDENCY — not integrated

The app source contains constants and settings for Termux companion apps (`Termux:API`, `Termux:Boot`, `Termux:Float`, `Termux:Styling`, `Termux:Tasker`, and `Termux:Widget`). These are not standalone PrimeTech companion packages and are not being rebuilt or integrated in this phase. Related source namespaces and optional preference entries remain unchanged.

### UNKNOWN — investigate before changing

- Hard-coded `com.termux` values in comments, documentation, tests, and diagnostic messages may be descriptive only.
- Any `com.termux` values inside downloaded/generated bootstrap archives cannot be audited until the archives are obtained and inspected.
- The app's external `termux-am` library dependency is a shared runtime implementation dependency, not evidence that the installed application ID must remain `com.termux`.

## Coexistence conclusion

The Android package identity, dynamic authorities, permission/action identities, launcher targets, and Java-derived app storage paths are separated from official Termux. The unchanged `com.termux.*` namespaces do not create an Android package collision. Full userspace independence is **not yet proven** until a PrimeTech-native bootstrap is built and validated.

## Repository preservation

The only repository in scope is `termux-app`. No files in `termux-packages`, `proot-distro`, or `termux-tools` were modified. No APK was uninstalled, overwritten, rebuilt, or replaced by this audit.
