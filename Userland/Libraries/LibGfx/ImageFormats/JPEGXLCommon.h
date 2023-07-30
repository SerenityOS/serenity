/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <AK/FixedArray.h>
#include <AK/String.h>

namespace Gfx {
/// 4.2 - Functions
ALWAYS_INLINE i32 unpack_signed(u32 u)
{
    if (u % 2 == 0)
        return static_cast<i32>(u / 2);
    return -static_cast<i32>((u + 1) / 2);
}
///

/// B.2 - Field types
// This is defined as a macro in order to get lazy-evaluated parameter
// Note that the lambda will capture your context by reference.
#define U32(d0, d1, d2, d3)                            \
    ({                                                 \
        u8 const selector = TRY(stream.read_bits(2));  \
        auto value = [&, selector]() -> ErrorOr<u32> { \
            if (selector == 0)                         \
                return (d0);                           \
            if (selector == 1)                         \
                return (d1);                           \
            if (selector == 2)                         \
                return (d2);                           \
            if (selector == 3)                         \
                return (d3);                           \
            VERIFY_NOT_REACHED();                      \
        }();                                           \
        TRY(value);                                    \
    })

ALWAYS_INLINE ErrorOr<u64> U64(LittleEndianInputBitStream& stream)
{
    u8 const selector = TRY(stream.read_bits(2));
    if (selector == 0)
        return 0;
    if (selector == 1)
        return 1 + TRY(stream.read_bits(4));
    if (selector == 2)
        return 17 + TRY(stream.read_bits(8));

    VERIFY(selector == 3);

    u64 value = TRY(stream.read_bits(12));
    u8 shift = 12;
    while (TRY(stream.read_bits(1)) == 1) {
        if (shift == 60) {
            value += TRY(stream.read_bits(4)) << shift;
            break;
        }
        value += TRY(stream.read_bits(8)) << shift;
        shift += 8;
    }

    return value;
}

ALWAYS_INLINE ErrorOr<f32> F16(LittleEndianInputBitStream& stream)
{
    u16 const bits16 = TRY(stream.read_bits(16));
    auto const biased_exp = (bits16 >> 10) & 0x1F;
    VERIFY(biased_exp != 31);
    return bit_cast<_Float16>(bits16);
}

template<Enum E>
ErrorOr<E> read_enum(LittleEndianInputBitStream& stream)
{
    return static_cast<E>(U32(0, 1, 2 + TRY(stream.read_bits(4)), 18 + TRY(stream.read_bits(6))));
}
///

ErrorOr<ByteBuffer> read_icc(LittleEndianInputBitStream& stream);

}
