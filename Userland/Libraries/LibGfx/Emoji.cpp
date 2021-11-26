/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Emoji.h>

namespace Gfx {

static HashMap<u32, RefPtr<Gfx::Bitmap>> s_emojis;

const Bitmap* Emoji::emoji_for_code_point(u32 code_point)
{
    auto it = s_emojis.find(code_point);
    if (it != s_emojis.end())
        return (*it).value.ptr();

    auto bitmap_or_error = Bitmap::try_load_from_file(String::formatted("/res/emoji/U+{:X}.png", code_point));
    if (bitmap_or_error.is_error()) {
        s_emojis.set(code_point, nullptr);
        return nullptr;
    }
    auto bitmap = bitmap_or_error.release_value();
    s_emojis.set(code_point, bitmap);
    return bitmap.ptr();
}

}
