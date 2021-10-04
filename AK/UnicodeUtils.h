/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace AK::UnicodeUtils {

constexpr bool is_unicode_control_code_point(u32 code_point)
{
    return code_point < 0x20 || (code_point >= 0x80 && code_point < 0xa0);
}

Optional<StringView> get_unicode_control_code_point_alias(u32);

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

}
