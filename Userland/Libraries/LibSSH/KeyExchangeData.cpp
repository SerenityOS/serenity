/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyExchangeData.h"
#include <LibCrypto/Hash/SHA2.h>
#include <LibSSH/DataTypes.h>

namespace SSH {

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
