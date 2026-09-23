# PrimeTech Repository Design

## Purpose

Describe the APT repository layout.

## Layout

pool/main/ dists/stable/main/binary-aarch64/

Metadata: - Origin: PrimeTech - Label: PrimeTech - Suite: stable -
Codename: stable

## Validation

Required checks:

-   apt update succeeds
-   Packages index generated
-   Release metadata matches repository
-   Packages contain PrimeTech paths

## Security

Repository packages must be built specifically for PrimeTech.
