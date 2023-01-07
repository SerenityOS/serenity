/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Error.h>
#include <AK/Forward.h>

namespace AK::UnicodeUtils {

template<typename Callback>
[[nodiscard]] constexpr int code_point_to_utf8(u32 code_point, Callback callback)
{
    if (code_point <= 0x7f) {
        callback((char)code_point);
        return 1;
    } else if (code_point <= 0x07ff) {
        callback((char)(((code_point >> 6) & 0x1f) | 0xc0));
        callback((char)(((code_point >> 0) & 0x3f) | 0x80));
        return 2;
    } else if (code_point <= 0xffff) {
        callback((char)(((code_point >> 12) & 0x0f) | 0xe0));
        callback((char)(((code_point >> 6) & 0x3f) | 0x80));
        callback((char)(((code_point >> 0) & 0x3f) | 0x80));
        return 3;
    } else if (code_point <= 0x10ffff) {
        callback((char)(((code_point >> 18) & 0x07) | 0xf0));
        callback((char)(((code_point >> 12) & 0x3f) | 0x80));
        callback((char)(((code_point >> 6) & 0x3f) | 0x80));
        callback((char)(((code_point >> 0) & 0x3f) | 0x80));
        return 4;
    }
    return -1;
}

template<FallibleFunction<char> Callback>
[[nodiscard]] ErrorOr<int> try_code_point_to_utf8(u32 code_point, Callback&& callback)
{
    if (code_point <= 0x7f) {
        TRY(callback(static_cast<char>(code_point)));
        return 1;
    }
    if (code_point <= 0x07ff) {
        TRY(callback(static_cast<char>((((code_point >> 6) & 0x1f) | 0xc0))));
        TRY(callback(static_cast<char>((((code_point >> 0) & 0x3f) | 0x80))));
        return 2;
    }
    if (code_point <= 0xffff) {
        TRY(callback(static_cast<char>((((code_point >> 12) & 0x0f) | 0xe0))));
        TRY(callback(static_cast<char>((((code_point >> 6) & 0x3f) | 0x80))));
        TRY(callback(static_cast<char>((((code_point >> 0) & 0x3f) | 0x80))));
        return 3;
    }
    if (code_point <= 0x10ffff) {
        TRY(callback(static_cast<char>((((code_point >> 18) & 0x07) | 0xf0))));
        TRY(callback(static_cast<char>((((code_point >> 12) & 0x3f) | 0x80))));
        TRY(callback(static_cast<char>((((code_point >> 6) & 0x3f) | 0x80))));
        TRY(callback(static_cast<char>((((code_point >> 0) & 0x3f) | 0x80))));
        return 4;
    }
    return -1;
}

}
