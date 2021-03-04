/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Array.h>
#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
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
    Array<u8, 256> table {};
    for (size_t i = 0; i < alphabet.size(); ++i) {
        table[alphabet[i]] = i;
    }
    return table;
}

size_t calculate_base64_decoded_length(const StringView& input)
{
    return input.length() * 3 / 4;
}

size_t calculate_base64_encoded_length(ReadonlyBytes input)
{
    return ((4 * input.size() / 3) + 3) & ~3;
}

ByteBuffer decode_base64(const StringView& input)
{
    auto get = [&](const size_t offset, bool* is_padding = nullptr) -> u8 {
        constexpr auto table = make_lookup_table();
        if (offset >= input.length())
            return 0;
        if (input[offset] == '=') {
            if (is_padding)
                *is_padding = true;
            return 0;
        }
        return table[input[offset]];
    };

    Vector<u8> output;
    output.ensure_capacity(calculate_base64_decoded_length(input));

    for (size_t i = 0; i < input.length(); i += 4) {
        bool in2_is_padding = false;
        bool in3_is_padding = false;

        const u8 in0 = get(i);
        const u8 in1 = get(i + 1);
        const u8 in2 = get(i + 2, &in2_is_padding);
        const u8 in3 = get(i + 3, &in3_is_padding);

        const u8 out0 = (in0 << 2) | ((in1 >> 4) & 3);
        const u8 out1 = ((in1 & 0xf) << 4) | ((in2 >> 2) & 0xf);
        const u8 out2 = ((in2 & 0x3) << 6) | in3;

        output.append(out0);
        if (!in2_is_padding)
            output.append(out1);
        if (!in3_is_padding)
            output.append(out2);
    }

    return ByteBuffer::copy(output.data(), output.size());
}

String encode_base64(ReadonlyBytes input)
{
    constexpr auto alphabet = make_alphabet();
    StringBuilder output(calculate_base64_decoded_length(input));

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
