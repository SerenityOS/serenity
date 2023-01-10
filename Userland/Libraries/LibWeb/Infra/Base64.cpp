/*
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/CharacterTypes.h>
#include <AK/Error.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibWeb/Infra/Base64.h>
#include <LibWeb/Infra/CharacterTypes.h>

namespace Web::Infra {

// https://infra.spec.whatwg.org/#forgiving-base64
ErrorOr<ByteBuffer> decode_forgiving_base64(StringView input)
{
    // 1. Remove all ASCII whitespace from data.
    // FIXME: It is possible to avoid copying input here, it's just a bit tricky to remove the equal signs
    StringBuilder builder;
    for (auto character : input) {
        if (!is_ascii_whitespace(character))
            TRY(builder.try_append(character));
    }
    auto data = builder.string_view();

    // 2. If data’s code point length divides by 4 leaving no remainder, then:
    if (data.length() % 4 == 0) {
        // If data ends with one or two U+003D (=) code points, then remove them from data.
        if (data.ends_with("=="sv))
            data = data.substring_view(0, data.length() - 2);
        else if (data.ends_with('='))
            data = data.substring_view(0, data.length() - 1);
    }

    // 3. If data’s code point length divides by 4 leaving a remainder of 1, then return failure.
    if (data.length() % 4 == 1)
        return Error::from_string_literal("Invalid input length in forgiving base64 decode");

    // 4. If data contains a code point that is not one of
    //     U+002B (+), U+002F (/), ASCII alphanumeric
    // then return failure.
    for (auto point : data) {
        if (point != '+' && point != '/' && !is_ascii_alphanumeric(point))
            return Error::from_string_literal("Invalid character in forgiving base64 decode");
    }

    // 5. Let output be an empty byte sequence.
    // 6. Let buffer be an empty buffer that can have bits appended to it.
    Vector<u8> output;
    u32 buffer = 0;
    auto accumulated_bits = 0;

    auto add_to_buffer = [&](u8 number) {
        VERIFY(number < 64);
        u32 buffer_mask = number;

        if (accumulated_bits == 0)
            buffer_mask <<= 18;
        else if (accumulated_bits == 6)
            buffer_mask <<= 12;
        else if (accumulated_bits == 12)
            buffer_mask <<= 6;
        else if (accumulated_bits == 18)
            buffer_mask <<= 0;

        buffer |= buffer_mask;

        accumulated_bits += 6;
    };

    auto append_bytes = [&]() {
        output.append(static_cast<u8>((buffer & 0xff0000) >> 16));
        output.append(static_cast<u8>((buffer & 0xff00) >> 8));
        output.append(static_cast<u8>(buffer & 0xff));

        buffer = 0;
        accumulated_bits = 0;
    };

    auto alphabet_lookup_table = AK::base64_lookup_table();

    // 7. Let position be a position variable for data, initially pointing at the start of data.
    // 8. While position does not point past the end of data:
    for (auto point : data) {
        // 1. Find the code point pointed to by position in the second column of Table 1: The Base 64 Alphabet of RFC 4648.
        //    Let n be the number given in the first cell of the same row. [RFC4648]
        auto n = alphabet_lookup_table[point];
        VERIFY(n >= 0);

        // 2. Append the six bits corresponding to n, most significant bit first, to buffer.
        add_to_buffer(static_cast<u8>(n));

        // 3. buffer has accumulated 24 bits,
        if (accumulated_bits == 24) {
            // interpret them as three 8-bit big-endian numbers.
            // Append three bytes with values equal to those numbers to output, in the same order, and then empty buffer
            append_bytes();
        }
    }

    // 9. If buffer is not empty, it contains either 12 or 18 bits.
    VERIFY(accumulated_bits == 0 || accumulated_bits == 12 || accumulated_bits == 18);

    // If it contains 12 bits, then discard the last four and interpret the remaining eight as an 8-bit big-endian number.
    if (accumulated_bits == 12)
        output.append(static_cast<u8>((buffer & 0xff0000) >> 16));

    // If it contains 18 bits, then discard the last two and interpret the remaining 16 as two 8-bit big-endian numbers.
    // Append the one or two bytes with values equal to those one or two numbers to output, in the same order.
    if (accumulated_bits == 18) {
        output.append(static_cast<u8>((buffer & 0xff0000) >> 16));
        output.append(static_cast<u8>((buffer & 0xff00) >> 8));
    }

    return ByteBuffer::copy(output);
}

}
