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

## Build

```bat
cd /d D:\workSpace\TerminalP
gradlew.bat :app:assembleDebug
```

The Gradle task downloads and verifies the upstream bootstrap archives before compiling. Generated APKs are written to:

```text
D:\workSpace\TerminalP\app\build\outputs\apk\debug\TerminalP-debug-<abi>.apk
```

A successful APK build does not prove that the embedded userspace is PrimeTech-native. The current upstream bootstrap contains Termux absolute shebangs and symlink targets; see `PRIMETECH_BOOTSTRAP_PLAN.md`.
