/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/Types.h>

namespace Gfx {

// Can decode bitstreams encoded with VP8's and VP9's arithmetic boolean encoder.
class BooleanDecoder {
public:
    static ErrorOr<BooleanDecoder> initialize(MaybeOwned<BigEndianInputBitStream> bit_stream, size_t size_in_bytes);

    /* (9.2) */
    static ErrorOr<BooleanDecoder> initialize_vp9(MaybeOwned<BigEndianInputBitStream> bit_stream, size_t size_in_bytes);

    ErrorOr<bool> read_bool(u8 probability);
    ErrorOr<u8> read_literal(u8 bits);

    ErrorOr<void> finish_decode_vp9();

private:
    BooleanDecoder(MaybeOwned<BigEndianInputBitStream>&& bit_stream, u8 value, u8 range, u64 bits_left)
        : m_bit_stream(move(bit_stream))
        , m_value(value)
        , m_range(range)
        , m_bits_left(bits_left)
    {
    }

    MaybeOwned<BigEndianInputBitStream> m_bit_stream;
    u8 m_value { 0 };
    u8 m_range { 0 };
    u64 m_bits_left { 0 };
};

}
