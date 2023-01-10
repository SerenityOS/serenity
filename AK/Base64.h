/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

constexpr Array base64_alphabet = {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
    'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
    'w', 'x', 'y', 'z', '0', '1', '2', '3',
    '4', '5', '6', '7', '8', '9', '+', '/'
};

consteval auto base64_lookup_table()
{
    Array<i16, 256> table;
    table.fill(-1);
    for (size_t i = 0; i < base64_alphabet.size(); ++i) {
        table[base64_alphabet[i]] = static_cast<i16>(i);
    }
    return table;
}

[[nodiscard]] size_t calculate_base64_decoded_length(StringView);

[[nodiscard]] size_t calculate_base64_encoded_length(ReadonlyBytes);

[[nodiscard]] ErrorOr<ByteBuffer> decode_base64(StringView);

[[nodiscard]] ErrorOr<String> encode_base64(ReadonlyBytes);
}

#if USING_AK_GLOBALLY
using AK::decode_base64;
using AK::encode_base64;
#endif
