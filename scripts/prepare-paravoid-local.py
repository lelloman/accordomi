#!/usr/bin/env python3
"""Create ignored development keys for local Paravoid shell builds."""

import base64
import json
import subprocess
from pathlib import Path


keys = Path(__file__).resolve().parents[1] / "app/paravoid/keys"
keys.mkdir(parents=True, exist_ok=True)
trust = {
    "version": 1,
    "applicationId": "com.lelloman.accordomi",
    "minimumPayloadVersion": 1,
    "minimumHeadRevision": 1,
}
for role, key_id in (("release", "accordomi-v1"), ("head", "head"), ("grant", "grant")):
    path = keys / f"{role}.der"
    if not path.exists():
        subprocess.run([
            "openssl", "genpkey", "-algorithm", "RSA",
            "-pkeyopt", "rsa_keygen_bits:3072", "-outform", "DER",
            "-out", str(path),
        ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        path.chmod(0o600)
    public = subprocess.check_output(
        ["openssl", "pkey", "-inform", "DER", "-in", str(path), "-pubout", "-outform", "DER"],
        stderr=subprocess.DEVNULL,
    )
    trust[f"{role}Keys"] = {key_id: base64.b64encode(public).decode("ascii")}
(keys / "trust.json").write_text(json.dumps(trust, sort_keys=True, separators=(",", ":")) + "\n")
print(f"PARAVOID_SIGNING_KEY={keys / 'release.der'}")
print(f"PARAVOID_TRUST_POLICY={keys / 'trust.json'}")
