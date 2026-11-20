#!/usr/bin/env python3
"""Extract the signing certificate SHA1 from an APK signed with APK Signature Scheme v2.

Reads the APK Signing Block (ID 0x7109871a), takes the first signer's first
certificate (DER), and prints its SHA1 fingerprint in both bare and
colon-separated uppercase forms.

Layout (little-endian uint32 length prefixes everywhere):
  v2 block value = [signers_seq_len][signer_len][signer]
  signer         = [signed_data_len][signed_data][signatures][public_key]
  signed_data    = [digests][certificates][additional_attributes]
  digests        = [len][entries: [len][algo_id][digest_len][digest] ...]
  certificates   = [len][entries: [len][DER cert bytes] ...]
"""
import hashlib
import struct
import sys

V2_BLOCK_ID = 0x7109871A
MAGIC = b"APK Sig Block 42"


def main(path: str) -> None:
    with open(path, "rb") as f:
        data = f.read()

    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0:
        raise SystemExit("EOCD record not found")
    cd_offset = struct.unpack_from("<I", data, eocd + 16)[0]

    if data[cd_offset - 16:cd_offset] != MAGIC:
        raise SystemExit("APK Signing Block magic not found before central directory")

    block_size = struct.unpack_from("<Q", data, cd_offset - 24)[0]
    # Total block length is block_size + 8 (the leading size field itself
    # is excluded from the stored value); the block ends at cd_offset.
    block_start = cd_offset - block_size - 8
    if struct.unpack_from("<Q", data, block_start)[0] != block_size:
        raise SystemExit("APK Signing Block size mismatch")

    # Walk the ID-value pairs inside the block.
    v2 = None
    pos = block_start + 8
    end = cd_offset - 24
    while pos < end:
        pair_len = struct.unpack_from("<Q", data, pos)[0]
        pair_id = struct.unpack_from("<I", data, pos + 8)[0]
        if pair_id == V2_BLOCK_ID:
            v2 = data[pos + 12:pos + 8 + pair_len]
        pos += 8 + pair_len
    if v2 is None:
        raise SystemExit("No v2 signature block (ID 0x7109871a) found")

    # Signers sequence, then first signer.
    p = 0
    seq_len = struct.unpack_from("<I", v2, p)[0]
    p += 4
    if seq_len != len(v2) - 4:
        raise SystemExit(f"Signers sequence length mismatch: {seq_len} != {len(v2) - 4}")
    signer_len = struct.unpack_from("<I", v2, p)[0]
    p += 4
    signer = v2[p:p + signer_len]

    # Signed data.
    q = 0
    sd_len = struct.unpack_from("<I", signer, q)[0]
    q += 4
    signed_data = signer[q:q + sd_len]

    # Skip digests, then read certificates.
    r = 0
    digests_len = struct.unpack_from("<I", signed_data, r)[0]
    r += 4 + digests_len
    certs_len = struct.unpack_from("<I", signed_data, r)[0]
    r += 4
    cert_len = struct.unpack_from("<I", signed_data, r)[0]
    r += 4
    cert = signed_data[r:r + cert_len]
    if len(cert) != cert_len or cert[0] != 0x30:
        raise SystemExit("Certificate bytes do not look like DER")

    sha1 = hashlib.sha1(cert).hexdigest().upper()
    colon = ":".join(sha1[i:i + 2] for i in range(0, len(sha1), 2))
    print(f"SHA1 (bare):  {sha1}")
    print(f"SHA1 (colon): {colon}")


if __name__ == "__main__":
    main(sys.argv[1])
