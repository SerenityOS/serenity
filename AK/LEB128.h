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
        // Note: We read into a u64 to simplify the parsing logic;
        //    result is range checked into ValueType after parsing.
        static_assert(sizeof(ValueType) <= sizeof(u64), "Error checking logic assumes 64 bits or less!");

        i64 temp = 0;
        size_t num_bytes = 0;
        u8 byte = 0;
        ValueType result {};

        do {
            if (stream.is_eof())
                return Error::from_string_literal("Stream reached end-of-file while reading LEB128 value");

            byte = TRY(stream.read_value<u8>());

            // note: 64 bit assumptions!
            u64 masked_byte = byte & ~(1 << 7);
            bool const shift_too_large_for_result = num_bytes * 7 >= 64;
            if (shift_too_large_for_result)
                return Error::from_string_literal("Read value contains more bits than fit the chosen ValueType");

            bool const shift_too_large_for_byte = (num_bytes * 7) == 63 && masked_byte != 0x00 && masked_byte != 0x7Fu;
            if (shift_too_large_for_byte)
                return Error::from_string_literal("Read byte is too large to fit the chosen ValueType");

            temp = (temp) | (masked_byte << (num_bytes * 7));
            ++num_bytes;
        } while (byte & (1 << 7));

        if ((num_bytes * 7) < 64 && (byte & 0x40)) {
            // sign extend
            temp |= ((u64)(-1) << (num_bytes * 7));
        }

        // Now that we've accumulated into an i64, make sure it fits into result
        if constexpr (sizeof(ValueType) < sizeof(u64)) {
            if (temp > NumericLimits<ValueType>::max() || temp < NumericLimits<ValueType>::min())
                return Error::from_string_literal("Temporary value does not fit the result type");
        }

        result = static_cast<ValueType>(temp);

        return LEB128<ValueType> { result };
    }

private:
    ValueType m_value { 0 };
};

}

#if USING_AK_GLOBALLY
using AK::LEB128;
#endif
