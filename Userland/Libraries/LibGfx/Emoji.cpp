/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Emoji.h>

namespace Gfx {

// https://unicode.org/reports/tr51/
// https://unicode.org/emoji/charts/emoji-list.html
// https://unicode.org/emoji/charts/emoji-zwj-sequences.html

static HashMap<Span<u32>, RefPtr<Gfx::Bitmap>> s_emojis;

Bitmap const* Emoji::emoji_for_code_point(u32 code_point)
{
    return emoji_for_code_points(Array { code_point });
}

Bitmap const* Emoji::emoji_for_code_points(Span<u32> const& code_points)
{
    auto it = s_emojis.find(code_points);
    if (it != s_emojis.end())
        return (*it).value.ptr();

    auto basename = String::join('_', code_points, "U+{:X}");
    auto bitmap_or_error = Bitmap::try_load_from_file(String::formatted("/res/emoji/{}.png", basename));
    if (bitmap_or_error.is_error()) {
        s_emojis.set(code_points, nullptr);
        return nullptr;
    }
    auto bitmap = bitmap_or_error.release_value();
    s_emojis.set(code_points, bitmap);
    return bitmap.ptr();
}

}
