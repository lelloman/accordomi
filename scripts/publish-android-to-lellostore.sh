#!/usr/bin/env bash

set -euo pipefail

# Defaults from the existing LelloStore publisher configuration.
# Environment variables and publisher CLI options can override these values.
export LELLOSTORE_URL="${LELLOSTORE_URL:-https://store.lelloman.com}"
export LELLOSTORE_OIDC_ISSUER="${LELLOSTORE_OIDC_ISSUER:-https://auth.lelloman.com}"
export LELLOSTORE_CLIENT_ID="${LELLOSTORE_CLIENT_ID:-22cd4a2d-a771-41e3-b76e-3f83ff8e9bbf}"

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
SIGNING_PROPERTIES="$REPOSITORY_DIR/signing.properties"
ARTIFACT="$REPOSITORY_DIR/app/build/outputs/apk/normal/storeRelease/app-normal-storeRelease.apk"

if [[ ! -f "$SIGNING_PROPERTIES" ]]; then
    echo "Missing Android release signing configuration: $SIGNING_PROPERTIES" >&2
    echo "Copy signing.properties.example and configure the release keystore." >&2
    exit 1
fi

if [[ -n "${LELLOSTORE_PUBLISHER:-}" ]]; then
    PUBLISHER="$LELLOSTORE_PUBLISHER"
elif [[ -x "$REPOSITORY_DIR/../lellostore/scripts/publish-to-lellostore.py" ]]; then
    PUBLISHER="$REPOSITORY_DIR/../lellostore/scripts/publish-to-lellostore.py"
elif [[ -x "${HOME}/lelloprojects/lellostore/scripts/publish-to-lellostore.py" ]]; then
    PUBLISHER="${HOME}/lelloprojects/lellostore/scripts/publish-to-lellostore.py"
else
    echo "Could not find the authoritative LelloStore publisher." >&2
    echo "Set LELLOSTORE_PUBLISHER to scripts/publish-to-lellostore.py in a LelloStore checkout." >&2
    exit 1
fi

if [[ ! -x "$PUBLISHER" ]]; then
    echo "LelloStore publisher is not executable: $PUBLISHER" >&2
    exit 1
fi

echo "Building signed Accordomi release APK..."
(
    cd "$REPOSITORY_DIR"
    ./gradlew :app:assembleNormalStoreRelease
)

if [[ ! -s "$ARTIFACT" ]]; then
    echo "Expected signed release APK was not produced: $ARTIFACT" >&2
    exit 1
fi

ARTIFACT_SIZE=$(stat --format='%s' "$ARTIFACT")
echo "Artifact: $ARTIFACT"
echo "Variant:  normalStoreRelease"
echo "Size:     $ARTIFACT_SIZE bytes"

"$PUBLISHER" upload "$ARTIFACT" --publish "$@"
