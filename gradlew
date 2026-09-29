#!/bin/sh

# H-Launcher Gradle bootstrap wrapper.
# It downloads the pinned Gradle distribution on first use, then delegates
# every argument to that distribution.

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
GRADLE_VERSION="8.9"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_ROOT="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
GRADLE_HOME="$DIST_ROOT/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

download_distribution() {
    mkdir -p "$DIST_ROOT"
    TMP_DIR=$(mktemp -d "$DIST_ROOT/bootstrap.XXXXXX")
    ZIP_PATH="$TMP_DIR/gradle.zip"

    cleanup() {
        rm -rf "$TMP_DIR"
    }
    trap cleanup EXIT INT TERM

    echo "Downloading Gradle ${GRADLE_VERSION}..."
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --retry 3 --output "$ZIP_PATH" "$DIST_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget --quiet --output-document="$ZIP_PATH" "$DIST_URL"
    else
        echo "Error: curl or wget is required to bootstrap Gradle." >&2
        exit 1
    fi

    if ! command -v unzip >/dev/null 2>&1; then
        echo "Error: unzip is required to bootstrap Gradle." >&2
        exit 1
    fi

    unzip -q "$ZIP_PATH" -d "$TMP_DIR"
    rm -rf "$GRADLE_HOME"
    mv "$TMP_DIR/gradle-${GRADLE_VERSION}" "$GRADLE_HOME"
}

if [ ! -x "$GRADLE_BIN" ]; then
    download_distribution
fi

exec "$GRADLE_BIN" "$@"