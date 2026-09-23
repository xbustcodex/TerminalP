# PrimeTech Bootstrap Design

## Purpose

Define the custom Android bootstrap system used by PrimeTech Terminal.

## Architecture

PrimeTech replaces the default Termux bootstrap source with a controlled
PrimeTech repository.

Components: - Android application - ARM64 bootstrap archive - PrimeTech
APT repository - Package metadata - First boot initialization

## Current State

Verified: - Bootstrap builds successfully. - ARM64 bootstrap
generated. - Repository source points to PrimeTech. - Package prefix
uses com.primetech.terminal.

## Rules

-   Do not reintroduce official Termux repository URLs.
-   Keep package paths under PrimeTech namespace.
-   Validate every bootstrap before release.
