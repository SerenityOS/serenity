/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Types.h>

namespace Gfx {

class Bitmap;

class Emoji {
public:
    static void set_emoji_lookup_path(String);

    static Gfx::Bitmap const* emoji_for_code_point(u32 code_point);
    static Gfx::Bitmap const* emoji_for_code_points(ReadonlySpan<u32> const&);
    static Gfx::Bitmap const* emoji_for_code_point_iterator(Utf8CodePointIterator&);
    static Gfx::Bitmap const* emoji_for_code_point_iterator(Utf32CodePointIterator&);
};

}
