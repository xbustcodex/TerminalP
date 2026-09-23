#!/bin/sh
#
# fetch-bootstrap.sh - acquire the validated PrimeTech ARM64 bootstrap.
#
#   fetch:   scripts/fetch-bootstrap.sh
#   verify:  scripts/fetch-bootstrap.sh --verify-only
#
# Flow:
#   manifest -> download release asset -> verify SHA-256 (and size)
#            -> atomically place at the Gradle build-input path -> gradlew build
#
# This script is deliberately separate from Gradle so a normal Android build
# never silently downloads network content. app/build.gradle independently
# re-validates the SHA-256 and still fails the build on any mismatch.
#
# Failure behaviour is fail-closed: hash mismatch, truncation, HTTP failure or
# a missing artifact abort with a non-zero status and no build input is written.
# Another bootstrap is never substituted, and an upstream Termux bootstrap is
# never used as a fallback.

set -eu

SELF="$0"
MODE="fetch"
for arg in "$@"; do
    case "$arg" in
        --verify-only|-v) MODE="verify" ;;
        -h|--help) sed -n '2,22p' "$SELF" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "ERROR: unknown argument: $arg" >&2; exit 2 ;;
    esac
done

fail() {
    echo "ERROR: $*" >&2
    echo "FAIL-CLOSED: no build input was written." >&2
    exit 1
}

# --- locate repository root -------------------------------------------------
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$SELF")" && pwd)
if REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && git rev-parse --show-toplevel 2>/dev/null); then
    :
else
    REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
fi
MANIFEST="$SCRIPT_DIR/bootstrap-aarch64.manifest"
[ -f "$MANIFEST" ] || fail "manifest not found: $MANIFEST"

# --- read manifest (KEY=VALUE, one per line) --------------------------------
SOURCE_REPOSITORY=""; RELEASE_TAG=""; ASSET_NAME=""
EXPECTED_SHA256=""; EXPECTED_SIZE=""; DESTINATION=""
# Lines are read verbatim, then CR is stripped, so the manifest parses correctly
# whether it is checked out LF or CRLF (core.autocrlf) and whether it was edited
# in an editor that writes Windows line endings.
while IFS= read -r line || [ -n "$line" ]; do
    line=$(printf '%s' "$line" | tr -d '\r')
    case "$line" in
        ''|\#*) continue ;;
        *=*) key=${line%%=*}; val=${line#*=}
             case "$key" in
                 SOURCE_REPOSITORY) SOURCE_REPOSITORY=$val ;;
                 RELEASE_TAG)       RELEASE_TAG=$val ;;
                 ASSET_NAME)        ASSET_NAME=$val ;;
                 EXPECTED_SHA256)   EXPECTED_SHA256=$val ;;
                 EXPECTED_SIZE)     EXPECTED_SIZE=$val ;;
                 DESTINATION)       DESTINATION=$val ;;
             esac ;;
    esac
done < "$MANIFEST"

[ -n "$SOURCE_REPOSITORY" ] || fail "SOURCE_REPOSITORY missing from manifest"
[ -n "$RELEASE_TAG" ]       || fail "RELEASE_TAG missing from manifest"
[ -n "$ASSET_NAME" ]        || fail "ASSET_NAME missing from manifest"
[ -n "$EXPECTED_SHA256" ]   || fail "EXPECTED_SHA256 missing from manifest"
[ -n "$DESTINATION" ]       || fail "DESTINATION missing from manifest"
printf '%s' "$EXPECTED_SHA256" | grep -Eq '^[0-9a-f]{64}$' \
    || fail "EXPECTED_SHA256 is not a lowercase 64-hex SHA-256: $EXPECTED_SHA256"

# --- guard: the manifest and build.gradle must agree ------------------------
# build.gradle is the authoritative pin. If the two ever drift, refuse to
# proceed rather than risk weakening the build-time validation.
BUILD_GRADLE="$REPO_ROOT/app/build.gradle"
[ -f "$BUILD_GRADLE" ] || fail "app/build.gradle not found in $REPO_ROOT"
grep -qF "$EXPECTED_SHA256" "$BUILD_GRADLE" \
    || fail "manifest SHA-256 ($EXPECTED_SHA256) is not pinned in app/build.gradle; refusing to continue"

DEST="$REPO_ROOT/$DESTINATION"

sha256_of() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    elif command -v openssl >/dev/null 2>&1; then
        openssl dgst -sha256 "$1" | awk '{print $NF}'
    else
        fail "no SHA-256 tool available (need sha256sum, shasum or openssl)"
    fi
}

size_of() { wc -c < "$1" | tr -d '[:space:]'; }

verify_file() {
    f=$1
    actual=$(sha256_of "$f")
    if [ "$actual" != "$EXPECTED_SHA256" ]; then
        echo "  expected SHA-256 : $EXPECTED_SHA256" >&2
        echo "  actual   SHA-256 : $actual" >&2
        return 1
    fi
    if [ -n "$EXPECTED_SIZE" ]; then
        actual_size=$(size_of "$f")
        if [ "$actual_size" != "$EXPECTED_SIZE" ]; then
            echo "  expected size : $EXPECTED_SIZE bytes" >&2
            echo "  actual   size : $actual_size bytes (truncated?)" >&2
            return 1
        fi
    fi
    return 0
}

# --- already present? -------------------------------------------------------
if [ -f "$DEST" ]; then
    echo "Existing build input: $DESTINATION"
    if verify_file "$DEST"; then
        echo "  SHA-256 OK       : $EXPECTED_SHA256"
        [ -n "$EXPECTED_SIZE" ] && echo "  size OK          : $EXPECTED_SIZE bytes"
        echo "Already present and verified - nothing to fetch."
        exit 0
    fi
    fail "existing file at $DESTINATION failed verification.
Refusing to accept or silently replace it. Remove the file manually if you
intend to re-acquire it, then re-run this script."
fi

[ "$MODE" = "fetch" ] || fail "build input missing: $DESTINATION (run without --verify-only)"

URL="https://github.com/$SOURCE_REPOSITORY/releases/download/$RELEASE_TAG/$ASSET_NAME"
command -v curl >/dev/null 2>&1 || fail "curl is required"

# --- optional authentication (never stored, never committed) ----------------
# Only used so a private repository's release asset can be read. Anonymous
# access is attempted when no credential is available.
# The token is held only in memory for this process: never written to disk,
# never echoed, and never part of the repository.
TOKEN_VALUE=""
if [ -n "${GITHUB_TOKEN:-}" ]; then
    TOKEN_VALUE=$GITHUB_TOKEN
elif [ -n "${GH_TOKEN:-}" ]; then
    TOKEN_VALUE=$GH_TOKEN
elif command -v gh >/dev/null 2>&1; then
    TOKEN_VALUE=$(gh auth token 2>/dev/null || true)
elif command -v git >/dev/null 2>&1; then
    CRED=$(printf 'protocol=https\nhost=github.com\n\n' | timeout 20 git credential fill 2>/dev/null || true)
    TOKEN_VALUE=$(printf '%s\n' "$CRED" | sed -n 's/^password=//p')
    unset CRED
fi

# -f fails on HTTP errors (404/403/406/5xx), -sS stays quiet but reports them,
# -L follows the asset redirect, --retry covers transient network failures.
# The header is passed as its own argument so curl parses it correctly.
http_get() {
    # $1 = output file, $2 = url
    if [ -n "$TOKEN_VALUE" ]; then
        curl -fsSL --retry 3 --retry-delay 2 --connect-timeout 20 --max-time 900 \
             -H "Authorization: Bearer $TOKEN_VALUE" -o "$1" "$2"
    else
        curl -fsSL --retry 3 --retry-delay 2 --connect-timeout 20 --max-time 900 \
             -o "$1" "$2"
    fi
}

mkdir -p "$(dirname -- "$DEST")"
TMP="$DEST.part.$$"
rm -f "$TMP"
trap 'rm -f "$TMP"' EXIT INT TERM

echo "Fetching : $URL"
echo "Release  : $SOURCE_REPOSITORY @ $RELEASE_TAG"
echo "Asset    : $ASSET_NAME"

if ! http_get "$TMP" "$URL"; then
    fail "download failed (HTTP error, unreachable host, or artifact unavailable): $URL"
fi
unset TOKEN_VALUE

[ -s "$TMP" ] || fail "download produced an empty file: $URL"

echo "Verifying SHA-256 ..."
if ! verify_file "$TMP"; then
    fail "SHA-256/size verification failed for the downloaded artifact.
The artifact was NOT accepted as a build input. Expected $EXPECTED_SHA256"
fi

# Same-filesystem rename so the build input appears atomically only once valid.
mv -f "$TMP" "$DEST"
trap - EXIT INT TERM

echo "SHA-256  : $(sha256_of "$DEST")"
[ -n "$EXPECTED_SIZE" ] && echo "size     : $(size_of "$DEST") bytes"
echo "Placed   : $DESTINATION"
echo "OK - validated bootstrap ready. Next: gradlew.bat :app:assembleDebug"
