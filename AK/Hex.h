/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/StringView.h>

#ifdef KERNEL
#    include <Kernel/Library/KString.h>
#else
#    include <AK/ByteString.h>
#endif

namespace AK {

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

ErrorOr<ByteBuffer> decode_hex(StringView);

#ifdef KERNEL
ErrorOr<NonnullOwnPtr<Kernel::KString>> encode_hex(ReadonlyBytes);
#else
ByteString encode_hex(ReadonlyBytes);
#endif

}

#if USING_AK_GLOBALLY
using AK::decode_hex;
using AK::decode_hex_digit;
using AK::encode_hex;
#endif
