/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/ByteBuffer.h>
#include <AK/Hex.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

Optional<ByteBuffer> decode_hex(const StringView& input)
{
    if ((input.length() % 2) != 0)
        return {};

    auto output = ByteBuffer::create_zeroed(input.length() / 2);

    for (size_t i = 0; i < input.length() / 2; ++i) {
        const auto c1 = decode_hex_digit(input[i * 2]);
        if (c1 >= 16)
            return {};

        const auto c2 = decode_hex_digit(input[i * 2 + 1]);
        if (c2 >= 16)
            return {};

        output[i] = (c1 << 4) + c2;
    }

    return output;
}

String encode_hex(const ReadonlyBytes input)
{
    StringBuilder output(input.size() * 2);

    for (auto ch : input)
        output.appendff("{:02x}", ch);

    return output.build();
}

}
