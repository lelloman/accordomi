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
ARTIFACT="$REPOSITORY_DIR/app/build/outputs/paravoid/paravoidAndroidRelease/shell.apk"
PAYLOAD="$REPOSITORY_DIR/app/build/outputs/paravoid/paravoidAndroidRelease/payload.vpk"
RELEASE_METADATA="$REPOSITORY_DIR/app/build/outputs/paravoid/paravoidAndroidRelease/release.json"
MAPPING="$REPOSITORY_DIR/app/build/outputs/paravoid/paravoidAndroidRelease/payload-mapping.txt"
PARAVOID_BASELINE_DIRECTORY="${PARAVOID_BASELINE_DIRECTORY:-${HOME}/.config/accordomi/paravoid-release/baseline-v6}"

PAYLOAD_VERSION=""
BUILD_ONLY=false
if [[ "${1:-}" == "--minified-payload-version" ]]; then
    PAYLOAD_VERSION="${2:-}"
    if [[ ! "$PAYLOAD_VERSION" =~ ^[1-9][0-9]*$ ]]; then
        echo "Expected a positive payload version after --minified-payload-version." >&2
        exit 1
    fi
    shift 2
    if [[ "${1:-}" == "--build-only" ]]; then
        BUILD_ONLY=true
        shift
    fi
fi

export PARAVOID_SIGNING_KEY="${PARAVOID_SIGNING_KEY:-${HOME}/.config/accordomi/paravoid-release/accordomi-release-2026.pk8}"
export PARAVOID_SIGNING_KEY_ID="${PARAVOID_SIGNING_KEY_ID:-accordomi-release-2026}"
export PARAVOID_TRUST_POLICY="${PARAVOID_TRUST_POLICY:-${HOME}/.config/accordomi/paravoid-release/trust.json}"
export PARAVOID_UPDATE_BASE_URL="${PARAVOID_UPDATE_BASE_URL:-https://store.lelloman.com/api/paravoid/}"

if [[ ! -f "$SIGNING_PROPERTIES" ]]; then
    echo "Missing Android release signing configuration: $SIGNING_PROPERTIES" >&2
    echo "Copy signing.properties.example and configure the release keystore." >&2
    exit 1
fi

for REQUIRED_FILE in "$PARAVOID_SIGNING_KEY" "$PARAVOID_TRUST_POLICY"; do
    if [[ ! -s "$REQUIRED_FILE" ]]; then
        echo "Missing Paravoid release configuration: $REQUIRED_FILE" >&2
        exit 1
    fi
done

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

if [[ -n "$PAYLOAD_VERSION" ]]; then
    BASELINE_CONTRACT="$PARAVOID_BASELINE_DIRECTORY/paravoidAndroidRelease/shell-contract.json"
    if [[ ! -s "$BASELINE_CONTRACT" ]]; then
        echo "Missing Paravoid shell baseline: $BASELINE_CONTRACT" >&2
        exit 1
    fi
    echo "Building signed minified Accordomi payload version $PAYLOAD_VERSION..."
    (
        cd "$REPOSITORY_DIR"
        ./gradlew :app:packageParavoidAndroidReleaseParavoidVpk \
            -PparavoidPayloadVersion="$PAYLOAD_VERSION" \
            -PparavoidMinifyPayload=true \
            -PparavoidBaselineDirectory="$PARAVOID_BASELINE_DIRECTORY"
    )
    for REQUIRED_ARTIFACT in "$PAYLOAD" "$RELEASE_METADATA" "$MAPPING"; do
        if [[ ! -s "$REQUIRED_ARTIFACT" ]]; then
            echo "Expected minified payload artifact was not produced: $REQUIRED_ARTIFACT" >&2
            exit 1
        fi
    done
    EXPECTED_CONTRACT=$(jq -r '.contractId' "$BASELINE_CONTRACT")
    ACTUAL_CONTRACT=$(jq -r '.body | @base64d | fromjson | .shellContractId' "$RELEASE_METADATA")
    ACTUAL_VERSION=$(jq -r '.body | @base64d | fromjson | .payloadVersion' "$RELEASE_METADATA")
    if [[ "$ACTUAL_CONTRACT" != "$EXPECTED_CONTRACT" || "$ACTUAL_VERSION" != "$PAYLOAD_VERSION" ]]; then
        echo "Payload version or shell contract differs from the requested release." >&2
        exit 1
    fi
    echo "Payload:  $PAYLOAD"
    echo "Mapping:  $MAPPING"
    echo "Version:  $ACTUAL_VERSION"
    echo "Contract: $ACTUAL_CONTRACT"
    echo "Size:     $(stat --format='%s' "$PAYLOAD") bytes"
    if [[ "$BUILD_ONLY" == true ]]; then
        exit 0
    fi
    "$PUBLISHER" upload-vpk com.lelloman.accordomi "$EXPECTED_CONTRACT" "$PAYLOAD" "$@"
    exit
fi

echo "Building signed Accordomi Paravoid shell and embedded payload..."
(
    cd "$REPOSITORY_DIR"
    ./gradlew :app:assembleParavoidAndroidRelease
)

for REQUIRED_ARTIFACT in "$ARTIFACT" "$PAYLOAD"; do
    if [[ ! -s "$REQUIRED_ARTIFACT" ]]; then
        echo "Expected Paravoid release artifact was not produced: $REQUIRED_ARTIFACT" >&2
        exit 1
    fi
done

ARTIFACT_SIZE=$(stat --format='%s' "$ARTIFACT")
echo "Artifact: $ARTIFACT"
echo "Payload:  $PAYLOAD"
echo "Variant:  paravoidAndroidRelease"
echo "Size:     $ARTIFACT_SIZE bytes"

"$PUBLISHER" upload "$ARTIFACT" "$@" --distribution-mode paravoid
