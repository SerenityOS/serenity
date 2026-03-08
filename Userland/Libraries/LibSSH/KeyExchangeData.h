/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibCrypto/Hash/HashFunction.h>
#include <LibSSH/DataTypes.h>

namespace SSH {

// 4.  ECDH Key Exchange
// https://datatracker.ietf.org/doc/html/rfc5656#section-4
struct KeyExchangeData {
    // FIXME: Add a reset method to erase sensitive information.
    ErrorOr<Crypto::Hash::Digest<256>> compute_sha_256() const;

    ByteBuffer client_identification_string {};
    ByteBuffer server_identification_string {};

    ByteBuffer client_key_init_payload {};
    ByteBuffer server_key_init_payload {};

    TypedBlob server_public_host_key {};

    ByteBuffer client_ephemeral_publickey {};
    ByteBuffer server_ephemeral_publickey {};

    ByteBuffer shared_secret {};
};

} // SSH
