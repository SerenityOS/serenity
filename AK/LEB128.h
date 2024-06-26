/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NumericLimits.h>
#include <AK/Stream.h>
#include <AK/Types.h>

namespace AK {

template<typename ValueType>
class [[gnu::packed]] LEB128 {
public:
    constexpr LEB128() = default;

    constexpr LEB128(ValueType value)
        : m_value(value)
    {
    }

    constexpr operator ValueType() const { return m_value; }

    static ErrorOr<LEB128<ValueType>> read_from_stream(Stream& stream)
    requires(Unsigned<ValueType>)
    {
        ValueType result {};
        size_t num_bytes = 0;
        while (true) {
            if (stream.is_eof())
                return Error::from_string_literal("Stream reached end-of-file while reading LEB128 value");

            auto byte = TRY(stream.read_value<u8>());

            ValueType masked_byte = byte & ~(1 << 7);
            bool const shift_too_large_for_result = num_bytes * 7 > sizeof(ValueType) * 8;
            if (shift_too_large_for_result)
                return Error::from_string_literal("Read value contains more bits than fit the chosen ValueType");

            bool const shift_too_large_for_byte = ((masked_byte << (num_bytes * 7)) >> (num_bytes * 7)) != masked_byte;
            if (shift_too_large_for_byte)
                return Error::from_string_literal("Read byte is too large to fit the chosen ValueType");

            result = (result) | (masked_byte << (num_bytes * 7));
            if (!(byte & (1 << 7)))
                break;
            ++num_bytes;
        }

        return LEB128<ValueType> { result };
    }

    static ErrorOr<LEB128<ValueType>> read_from_stream(Stream& stream)
    requires(Signed<ValueType>)
    {
        constexpr auto BITS = sizeof(ValueType) * 8;

        ValueType result = 0;
        u32 shift = 0;
        u8 byte = 0;

        do {
            if (stream.is_eof())
                return Error::from_string_literal("Stream reached end-of-file while reading LEB128 value");
            byte = TRY(stream.read_value<u8>());
            result |= (ValueType)(byte & 0x7F) << shift;

            if (shift >= BITS - 7) {
                bool has_continuation = (byte & 0x80);
                ValueType sign_and_unused = (i8)(byte << 1) >> (BITS - shift);
                if (has_continuation)
                    return Error::from_string_literal("Read value contains more bits than fit the chosen ValueType");
                if (sign_and_unused != 0 && sign_and_unused != -1)
                    return Error::from_string_literal("Read byte is too large to fit the chosen ValueType");
                return LEB128<ValueType> { result };
            }

            shift += 7;
        } while (byte & 0x80);

        // Sign extend
        if (shift < BITS && (byte & 0x40))
            result |= ((ValueType)~0 << shift);

        return LEB128<ValueType> { result };
    }

private:
    ValueType m_value { 0 };
};

}

#if USING_AK_GLOBALLY
using AK::LEB128;
#endif
