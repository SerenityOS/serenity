/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Web::Infra {

// https://infra.spec.whatwg.org/#ascii-whitespace
constexpr bool is_ascii_whitespace(u32 code_point)
{
    // ASCII whitespace is U+0009 TAB, U+000A LF, U+000C FF, U+000D CR, or U+0020 SPACE.
    return code_point == '\t' || code_point == '\n' || code_point == '\f' || code_point == '\r' || code_point == ' ';
}

}
