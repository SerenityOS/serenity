/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
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

Bitmap const* Emoji::emoji_for_code_point_iterator(Utf8CodePointIterator& it)
{
    // NOTE: I'm sure this could be more efficient, e.g. by checking if each code point falls
    // into a certain range in the loop below (emojis, modifiers, variation selectors, ZWJ),
    // and bailing out early if not. Current worst case is 10 file lookups for any sequence of
    // code points (if the first glyph isn't part of the font in regular text rendering).

    constexpr size_t max_emoji_code_point_sequence_length = 10;

    Vector<u32, max_emoji_code_point_sequence_length> code_points;

    struct EmojiAndCodePoints {
        Bitmap const* emoji;
        Span<u32> code_points;
    };
    Vector<EmojiAndCodePoints, max_emoji_code_point_sequence_length> possible_emojis;

    // Determine all existing emojis for the longest possible ZWJ emoji sequence,
    // or until we run out of code points in the iterator.
    for (size_t i = 0; i < max_emoji_code_point_sequence_length; ++i) {
        auto code_point = it.peek(i);
        if (!code_point.has_value())
            break;
        code_points.append(*code_point);
        if (auto const* emoji = emoji_for_code_points(code_points))
            possible_emojis.empend(emoji, code_points);
    }

    if (possible_emojis.is_empty())
        return nullptr;

    // If we found one or more matches, return the longest, i.e. last. For example:
    // U+1F3F3 - white flag
    // U+1F3F3 U+FE0F U+200D U+1F308 - rainbow flag
    auto& [emoji, emoji_code_points] = possible_emojis.last();

    // Advance the iterator, so it's on the last code point of our found emoji and
    // whoever is iterating will advance to the next new code point.
    for (size_t i = 0; i < emoji_code_points.size() - 1; ++i)
        ++it;

    return emoji;
}

}
