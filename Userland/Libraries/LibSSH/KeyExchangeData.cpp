/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyExchangeData.h"
#include <LibCrypto/Hash/SHA2.h>
#include <LibSSH/DataTypes.h>

namespace SSH {

bool KeyExchangeData::is_valid_x25519_shared_secret(ReadonlyBytes shared_secret)
{
    // This KEX uses X25519, whose output is exactly 32 bytes. Accumulate every byte before
    // branching so the all-zero check does not stop at the first non-zero byte.
    if (shared_secret.size() != 32)
        return false;

    u8 nonzero_byte = 0;
    for (auto byte : shared_secret)
        nonzero_byte |= byte;

    return nonzero_byte != 0;
}

ErrorOr<Crypto::Hash::Digest<256>> KeyExchangeData::compute_sha_256() const
{
    AllocatingMemoryStream stream;

    TRY(encode_string(stream, client_identification_string));
    TRY(encode_string(stream, server_identification_string));

    TRY(encode_string(stream, client_key_init_payload));
    TRY(encode_string(stream, server_key_init_payload));

    TRY(server_public_host_key.encode(stream));

    TRY(encode_string(stream, client_ephemeral_publickey));
    TRY(encode_string(stream, server_ephemeral_publickey));

    TRY(encode_mpint(stream, shared_secret));

    auto final = TRY(stream.read_until_eof());

    return Crypto::Hash::SHA256::hash(final);
}

} // SSH
