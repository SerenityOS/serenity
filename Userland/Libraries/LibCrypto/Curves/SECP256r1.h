/*
 * Copyright (c) 2022, Michiel Visser <opensource@webmichiel.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/UFixedBigInt.h>
#include <LibCrypto/Curves/EllipticCurve.h>

namespace Crypto::Curves {

class SECP256r1 : public EllipticCurve {
public:
    size_t key_size() override { return 1 + 2 * 32; }
    ErrorOr<ByteBuffer> generate_private_key() override;
    ErrorOr<ByteBuffer> generate_public_key(ReadonlyBytes a) override;
    ErrorOr<ByteBuffer> compute_coordinate(ReadonlyBytes scalar_bytes, ReadonlyBytes point_bytes) override;
    ErrorOr<ByteBuffer> derive_premaster_key(ReadonlyBytes shared_point) override;

    ErrorOr<bool> verify(ReadonlyBytes hash, ReadonlyBytes pubkey, ReadonlyBytes signature);
};

}
