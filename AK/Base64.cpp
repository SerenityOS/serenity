/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/Base64.h>
#include <AK/CharacterTypes.h>
#include <AK/Error.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

size_t calculate_base64_decoded_length(StringView input)
{
    return input.length() * 3 / 4;
}

size_t calculate_base64_encoded_length(ReadonlyBytes input)
{
    return ((4 * input.size() / 3) + 3) & ~3;
}

ErrorOr<ByteBuffer> decode_base64(StringView input)
{
    auto alphabet_lookup_table = base64_lookup_table();

    auto get = [&](size_t& offset, bool* is_padding, bool& parsed_something) -> ErrorOr<u8> {
        while (offset < input.length() && is_ascii_space(input[offset]))
            ++offset;
        if (offset >= input.length())
            return 0;
        auto ch = static_cast<unsigned char>(input[offset++]);
        parsed_something = true;
        if (ch == '=') {
            if (!is_padding)
                return Error::from_string_literal("Invalid '=' character outside of padding in base64 data");
            *is_padding = true;
            return 0;
        }
        i16 result = alphabet_lookup_table[ch];
        if (result < 0)
            return Error::from_string_literal("Invalid character in base64 data");
        VERIFY(result < 256);
        return { result };
    };

    Vector<u8> output;
    output.ensure_capacity(calculate_base64_decoded_length(input));

    size_t offset = 0;
    while (offset < input.length()) {
        bool in2_is_padding = false;
        bool in3_is_padding = false;

        bool parsed_something = false;

        const u8 in0 = TRY(get(offset, nullptr, parsed_something));
        const u8 in1 = TRY(get(offset, nullptr, parsed_something));
        const u8 in2 = TRY(get(offset, &in2_is_padding, parsed_something));
        const u8 in3 = TRY(get(offset, &in3_is_padding, parsed_something));

        if (!parsed_something)
            break;

        const u8 out0 = (in0 << 2) | ((in1 >> 4) & 3);
        const u8 out1 = ((in1 & 0xf) << 4) | ((in2 >> 2) & 0xf);
        const u8 out2 = ((in2 & 0x3) << 6) | in3;

        output.append(out0);
        if (!in2_is_padding)
            output.append(out1);
        if (!in3_is_padding)
            output.append(out2);
    }

    return ByteBuffer::copy(output);
}

ErrorOr<String> encode_base64(ReadonlyBytes input)
{
    StringBuilder output(calculate_base64_encoded_length(input));

    auto get = [&](const size_t offset, bool* need_padding = nullptr) -> u8 {
        if (offset >= input.size()) {
            if (need_padding)
                *need_padding = true;
            return 0;
        }
        return input[offset];
    };

    for (size_t i = 0; i < input.size(); i += 3) {
        bool is_8bit = false;
        bool is_16bit = false;

        const u8 in0 = get(i);
        const u8 in1 = get(i + 1, &is_16bit);
        const u8 in2 = get(i + 2, &is_8bit);

        const u8 index0 = (in0 >> 2) & 0x3f;
        const u8 index1 = ((in0 << 4) | (in1 >> 4)) & 0x3f;
        const u8 index2 = ((in1 << 2) | (in2 >> 6)) & 0x3f;
        const u8 index3 = in2 & 0x3f;

        char const out0 = base64_alphabet[index0];
        char const out1 = base64_alphabet[index1];
        char const out2 = is_16bit ? '=' : base64_alphabet[index2];
        char const out3 = is_8bit ? '=' : base64_alphabet[index3];

        TRY(output.try_append(out0));
        TRY(output.try_append(out1));
        TRY(output.try_append(out2));
        TRY(output.try_append(out3));
    }

    return output.to_string();
}

// https://infra.spec.whatwg.org/#forgiving-base64
ErrorOr<ByteBuffer> decode_forgiving_base64(StringView input)
{
    // 1. Remove all ASCII whitespace from data.
    auto data = input.trim_whitespace();

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

    auto alphabet_lookup_table = base64_lookup_table();

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
