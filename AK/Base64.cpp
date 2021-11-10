/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Base64.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

static constexpr auto make_alphabet()
{
    Array alphabet = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/'
    };
    return alphabet;
}

static constexpr auto make_lookup_table()
{
    constexpr auto alphabet = make_alphabet();
    Array<i16, 256> table;
    table.fill(-1);
    for (size_t i = 0; i < alphabet.size(); ++i) {
        table[alphabet[i]] = i;
    }
    return table;
}

size_t calculate_base64_decoded_length(StringView input)
{
    return input.length() * 3 / 4;
}

size_t calculate_base64_encoded_length(ReadonlyBytes input)
{
    return ((4 * input.size() / 3) + 3) & ~3;
}

Optional<ByteBuffer> decode_base64(StringView input)
{
    auto get = [&](const size_t offset, bool* is_padding) -> Optional<u8> {
        constexpr auto table = make_lookup_table();
        if (offset >= input.length())
            return 0;
        if (input[offset] == '=') {
            if (!is_padding)
                return {};
            *is_padding = true;
            return 0;
        }
        i16 result = table[static_cast<unsigned char>(input[offset])];
        if (result < 0)
            return {};
        VERIFY(result < 256);
        return { result };
    };
#define TRY_GET(index, is_padding)                       \
    ({                                                   \
        auto _temporary_result = get(index, is_padding); \
        if (!_temporary_result.has_value())              \
            return {};                                   \
        _temporary_result.value();                       \
    })

    Vector<u8> output;
    output.ensure_capacity(calculate_base64_decoded_length(input));

    for (size_t i = 0; i < input.length(); i += 4) {
        bool in2_is_padding = false;
        bool in3_is_padding = false;

        const u8 in0 = TRY_GET(i, nullptr);
        const u8 in1 = TRY_GET(i + 1, nullptr);
        const u8 in2 = TRY_GET(i + 2, &in2_is_padding);
        const u8 in3 = TRY_GET(i + 3, &in3_is_padding);

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
    constexpr auto alphabet = make_alphabet();
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

        const u8 out0 = alphabet[index0];
        const u8 out1 = alphabet[index1];
        const u8 out2 = is_16bit ? '=' : alphabet[index2];
        const u8 out3 = is_8bit ? '=' : alphabet[index3];

        output.append(out0);
        output.append(out1);
        output.append(out2);
        output.append(out3);
    }

    return output.to_string();
}

}
