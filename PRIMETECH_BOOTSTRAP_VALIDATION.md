# TerminalP Bootstrap Validation

## ARM64 source artifact

`app/src/main/cpp/bootstrap-aarch64.zip` has SHA-256:

`cfc1fb4e01854851327545f75942dc47e70ec5e8fafd018fd1b4d6f7c8dc99d7`

Its `bin/login` begins with:

```text
#!/data/data/com.primetech.terminal/files/usr/bin/sh
```

## Root cause of the observed device failure

The previously installed APK was not carrying this source archive. Inspection of the generated ARM64 native library showed that its embedded ZIP contained:

```text
#!/data/data/com.termux/files/usr/bin/sh
```

The native embedding path is `termux-bootstrap-zip.S`, which uses `.incbin "bootstrap-aarch64.zip"`; therefore the stale content was present before Android extraction, not caused by file permissions, SELinux, ownership, or the Java extractor. The installed `login` mode was already executable (`0700`).

## Pipeline fix

`app/build.gradle` now treats the ARM64 PrimeTech archive as a required, checksum-verified local source artifact. If it is missing or has the wrong checksum, `downloadBootstraps` fails instead of downloading an upstream archive. Other ABIs retain their existing upstream behavior until independently rebuilt.

The APK/native intermediate must be inspected after each Windows build by extracting `lib/arm64-v8a/libtermux-bootstrap.so`, locating the ZIP payload, and checking `bin/login`. A clean uninstall of only `com.primetech.terminal` is required before runtime testing.

## Device verification

Not performed from this environment. Do not claim shell, apt, dpkg, or pkg success until the rebuilt APK is installed and tested on the device.
