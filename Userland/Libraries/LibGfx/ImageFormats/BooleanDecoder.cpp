/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>

#include "BooleanDecoder.h"

namespace Gfx {

ErrorOr<BooleanDecoder> BooleanDecoder::initialize(MaybeOwned<BigEndianInputBitStream> bit_stream, size_t size_in_bytes)
{
    VERIFY(bit_stream->is_aligned_to_byte_boundary());
    auto value = TRY(bit_stream->read_value<u8>());
    u8 range = 255;
    u64 bits_left = (8 * size_in_bytes) - 8;
    return BooleanDecoder { move(bit_stream), value, range, bits_left };
}

/* 9.2.1 */
ErrorOr<BooleanDecoder> BooleanDecoder::initialize_vp9(MaybeOwned<BigEndianInputBitStream> bit_stream, size_t size_in_bytes)
{
    BooleanDecoder decoder = TRY(initialize(move(bit_stream), size_in_bytes));
    if (TRY(decoder.read_bool(128)))
        return Error::from_string_literal("Range decoder marker was non-zero");
    return decoder;
}

/* 9.2.2 */
ErrorOr<bool> BooleanDecoder::read_bool(u8 probability)
{
    auto split = 1u + (((m_range - 1u) * probability) >> 8u);
    bool return_bool;

    if (m_value < split) {
        m_range = split;
        return_bool = false;
    } else {
        m_range -= split;
        m_value -= split;
        return_bool = true;
    }

    if (m_range < 128) {
        u8 bits_to_shift_into_range = count_leading_zeroes(m_range);

        if (bits_to_shift_into_range > m_bits_left)
            return Error::from_string_literal("Range decoder is out of data");

        m_range <<= bits_to_shift_into_range;
        m_value = (m_value << bits_to_shift_into_range) | TRY(m_bit_stream->read_bits<u8>(bits_to_shift_into_range));
        m_bits_left -= bits_to_shift_into_range;
    }

    return return_bool;
}

ErrorOr<u8> BooleanDecoder::read_literal(u8 bits)
{
    u8 return_value = 0;
    for (size_t i = 0; i < bits; i++) {
        return_value = (2 * return_value) + TRY(read_bool(128));
    }
    return return_value;
}

/* 9.2.3 */
ErrorOr<void> BooleanDecoder::finish_decode_vp9()
{
    while (m_bits_left > 0) {
        auto padding_read_size = min(m_bits_left, 64);
        auto padding_bits = TRY(m_bit_stream->read_bits(padding_read_size));
        m_bits_left -= padding_read_size;

        if (padding_bits != 0)
            return Error::from_string_literal("Range decoder has non-zero padding element");
    }

    // FIXME: It is a requirement of bitstream conformance that enough padding bits are inserted to ensure that the final coded byte of a frame is not equal to a superframe marker.
    //  A byte b is equal to a superframe marker if and only if (b & 0xe0)is equal to 0xc0, i.e. if the most significant 3 bits are equal to 0b110.
    return {};
}

}
