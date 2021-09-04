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

Bitmap const* Emoji::emoji_for_code_point(u32 code_point)
{
    auto it = s_emojis.find(code_point);
    if (it != s_emojis.end())
        return (*it).value.ptr();

    auto bitmap = Bitmap::try_load_from_file(String::formatted("/res/emoji/U+{:X}.png", code_point));
    if (!bitmap) {
        s_emojis.set(code_point, nullptr);
        return nullptr;
    }

    s_emojis.set(code_point, bitmap);
    return bitmap.ptr();
}

}
