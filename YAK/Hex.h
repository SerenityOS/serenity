/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/Optional.h>
#include <YAK/String.h>
#include <YAK/StringView.h>

namespace YAK {

constexpr u8 decode_hex_digit(char digit)
{
    if (digit >= '0' && digit <= '9')
        return digit - '0';
    if (digit >= 'a' && digit <= 'f')
        return 10 + (digit - 'a');
    if (digit >= 'A' && digit <= 'F')
        return 10 + (digit - 'A');
    return 255;
}

Optional<ByteBuffer> decode_hex(const StringView&);

String encode_hex(ReadonlyBytes);

}

using YAK::decode_hex;
using YAK::decode_hex_digit;
using YAK::encode_hex;
