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

#include <AK/ByteBuffer.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

static constexpr u8 s_alphabet[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
static u8 s_table[256] = {};
static bool s_initialized = false;

static void build_lookup_table_if_needed()
{
    if (s_initialized)
        return;
    for (size_t i = 0; i < sizeof(s_alphabet) - 1; ++i)
        s_table[s_alphabet[i]] = i;
    s_initialized = true;
}

ByteBuffer decode_base64(const StringView& input)
{
    build_lookup_table_if_needed();

    auto get = [&](size_t offset, bool* is_padding = nullptr) -> u8 {
        if (offset >= input.length())
            return 0;
        if (input[offset] == '=') {
            if (is_padding)
                *is_padding = true;
            return 0;
        }
        return s_table[(u8)input[offset]];
    };

    Vector<u8> output;

    for (size_t i = 0; i < input.length(); i += 4) {
        bool in2_is_padding = false;
        bool in3_is_padding = false;

        u8 in0 = get(i);
        u8 in1 = get(i + 1);
        u8 in2 = get(i + 2, &in2_is_padding);
        u8 in3 = get(i + 3, &in3_is_padding);

        u8 out0 = (in0 << 2) | ((in1 >> 4) & 3);
        u8 out1 = ((in1 & 0xf) << 4) | ((in2 >> 2) & 0xf);
        u8 out2 = ((in2 & 0x3) << 6) | in3;

        output.append(out0);
        if (!in2_is_padding)
            output.append(out1);
        if (!in3_is_padding)
            output.append(out2);
    }

    return ByteBuffer::copy(output.data(), output.size());
}

}
