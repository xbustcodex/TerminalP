# PrimeTech Package Build Rules

## Package Requirements

All packages must:

-   Build for aarch64
-   Use PrimeTech prefix
-   Avoid com.termux paths
-   Produce valid Debian metadata

## Validation Commands

dpkg-deb -I package.deb dpkg-deb -c package.deb

Check for: - correct architecture - correct paths - correct dependencies

## Artifact Policy

Never publish unverified packages.
