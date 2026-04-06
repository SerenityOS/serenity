/*
 * Copyright (c) 2022, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibCrypto/Curves/EllipticCurve.h>

namespace Crypto::Curves::Ed25519 {

inline size_t key_size() { return 32; }
inline size_t signature_size() { return 64; }
ErrorOr<ByteBuffer> generate_private_key();
ErrorOr<ByteBuffer> generate_public_key(ReadonlyBytes private_key);

ErrorOr<ByteBuffer> sign(ReadonlyBytes public_key, ReadonlyBytes private_key, ReadonlyBytes message);
bool verify(ReadonlyBytes public_key, ReadonlyBytes signature, ReadonlyBytes message);

}
