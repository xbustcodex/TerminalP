# PrimeTech Storage Initialization Design

## Purpose

Document Android shared storage behaviour.

## Current Behaviour

Storage setup remains based on Termux compatibility:

Command: termux-setup-storage

Creates: \~/storage/shared \~/storage/downloads \~/storage/dcim
\~/storage/pictures \~/storage/music \~/storage/movies

## Design Decision

Do not silently remove user control.

Storage permissions must be explicit.
