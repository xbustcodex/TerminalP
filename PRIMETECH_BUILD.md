# TerminalP build

This directory builds a standalone Android application:

- Application ID: `com.primetech.terminal`
- Visible name: `TerminalP`
- Official Termux is not modified or required.

## Windows requirements

Install a 64-bit JDK compatible with the Android Gradle Plugin used by this checkout. JDK 17 is the supported baseline; JDK 21 is also suitable if accepted by the installed Gradle/AGP combination. Set `JAVA_HOME` to the JDK directory and add `%JAVA_HOME%\bin` to `Path`.

Verify in a new Command Prompt:

```bat
java -version
javac -version
```

Install the Android SDK with the platform and build tools versions specified in `gradle.properties`, plus NDK `29.0.14206865`. Set `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) to the SDK directory and ensure `platform-tools`, `cmdline-tools\latest\bin`, and the relevant SDK build-tools directory are on `Path`.

## Bootstrap acquisition (required first)

The validated PrimeTech ARM64 bootstrap archive is **not** stored in Git history.
A fresh clone must acquire it explicitly before building:

```sh
# from Git Bash (Windows) or any POSIX shell
scripts/fetch-bootstrap.sh
```

This downloads the release asset, verifies
SHA-256 `97d5bbb3e2db080e076e52966294197624ab7c4bf0ce0437f909a5ee57da83ba`, and
places it at `app/src/main/cpp/bootstrap-aarch64.zip`. It fails closed on a hash
mismatch, truncation, HTTP failure or a missing artifact, and never substitutes
another bootstrap or an upstream Termux one. Full release/tag/asset identity:
`docs/BOOTSTRAP_ACQUISITION.md`.

## Build

```bat
cd /d D:\workSpace\TerminalP
gradlew.bat :app:assembleDebug
```

The Gradle task verifies the bootstrap archives before compiling, and fetches the
non-PrimeTech upstream archives for the remaining ABIs. It never regenerates or
silently downloads the ARM64 PrimeTech bootstrap. Generated APKs are written to:

```text
D:\workSpace\TerminalP\app\build\outputs\apk\debug\TerminalP-debug-<abi>.apk
```

A successful APK build does not prove that the embedded userspace is PrimeTech-native. The current upstream bootstrap contains Termux absolute shebangs and symlink targets; see `PRIMETECH_BOOTSTRAP_PLAN.md`.
