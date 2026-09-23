#!/usr/bin/env bash
# Source this file before invoking the local-source bootstrap builder.
# It is intentionally separate from the upstream termux-packages checkout.

# This file is a declarative input for the wrapper below. The upstream
# properties file currently assigns these variables unconditionally, so a
# wrapper must source this file and apply the values before properties.sh.
export TERMUX_APP__PACKAGE_NAME="com.primetech.terminal"
export TERMUX_APP_PACKAGE="$TERMUX_APP__PACKAGE_NAME"

# properties.sh derives TERMUX_APP__DATA_DIR, TERMUX__ROOTFS, TERMUX__HOME,
# and TERMUX__PREFIX from TERMUX_APP__PACKAGE_NAME. These explicit exports are
# useful for audit logs and for wrappers that inspect the build identity.
export PRIMETECH_PACKAGE_NAME="$TERMUX_APP__PACKAGE_NAME"
export PRIMETECH_PREFIX="/data/data/${TERMUX_APP__PACKAGE_NAME}/files/usr"
export PRIMETECH_HOME="/data/data/${TERMUX_APP__PACKAGE_NAME}/files/home"
