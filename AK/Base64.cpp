/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Base64.h>
#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

static constexpr Array alphabet = {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
    'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
    'w', 'x', 'y', 'z', '0', '1', '2', '3',
    '4', '5', '6', '7', '8', '9', '+', '/'
};

static consteval auto make_lookup_table()
{
    Array<i16, 256> table;
    table.fill(-1);
    for (size_t i = 0; i < alphabet.size(); ++i) {
        table[alphabet[i]] = static_cast<i16>(i);
    }
    return table;
}

static constexpr auto alphabet_lookup_table = make_lookup_table();

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

String encode_base64(ReadonlyBytes input)
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

        char const out0 = alphabet[index0];
        char const out1 = alphabet[index1];
        char const out2 = is_16bit ? '=' : alphabet[index2];
        char const out3 = is_8bit ? '=' : alphabet[index3];

        output.append(out0);
        output.append(out1);
        output.append(out2);
        output.append(out3);
    }

    return output.to_string();
}

}
