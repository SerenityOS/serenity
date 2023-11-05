/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/TextElision.h>
#include <LibGfx/TextWrapping.h>

namespace Gfx {

// FIXME: This currently isn't an ideal way of doing things; ideally, TextLayout
// would be doing the rendering by painting individual glyphs. However, this
// would regress our Unicode bidirectional text support. Therefore, fixing this
// requires:
// - Moving the bidirectional algorithm either here, or some place TextLayout
//   can access;
// - Making TextLayout render the given text into something like a Vector<Line>
//   where:
//   using Line = Vector<DirectionalRun>;
//   struct DirectionalRun {
//       Utf32View glyphs;
//       Vector<int> advance;
//       TextDirection direction;
//   };
// - Either;
//   a) Making TextLayout output these Lines directly using a given Painter, or
//   b) Taking the Lines from TextLayout and painting each glyph.
class TextLayout {
public:
    TextLayout(Gfx::Font const& font, Utf8View const& text, FloatRect const& rect)
        : m_font(font)
        , m_font_metrics(font.pixel_metrics())
        , m_text(text)
        , m_rect(rect)
    {
    }

    Vector<DeprecatedString, 32> lines(TextElision elision, TextWrapping wrapping) const
    {
        return wrap_lines(elision, wrapping);
    }

    FloatRect bounding_rect(TextWrapping) const;

private:
    Vector<DeprecatedString, 32> wrap_lines(TextElision, TextWrapping) const;
    DeprecatedString elide_text_from_right(Utf8View) const;

    Font const& m_font;
    FontPixelMetrics m_font_metrics;
    Utf8View m_text;
    FloatRect m_rect;
};

inline bool should_paint_as_space(u32 code_point)
{
    return is_ascii_space(code_point) || code_point == 0xa0;
}

enum class IncludeLeftBearing {
    Yes,
    No
};

struct GlyphPosition {
    FloatPoint position;
    float glyph_width;
    AK::Utf8CodePointIterator& it;
};

template<typename Callback>
void for_each_glyph_position(FloatPoint start, Utf8View text, Font const& font, Callback callback, IncludeLeftBearing include_left_bearing = IncludeLeftBearing::No)
{
    float space_width = font.glyph_width(' ') + font.glyph_spacing();

    u32 last_code_point = 0;

    auto point = start;
    point.translate_by(0, -font.pixel_metrics().ascent);

    for (auto code_point_iterator = text.begin(); code_point_iterator != text.end(); ++code_point_iterator) {
        auto code_point = *code_point_iterator;
        if (should_paint_as_space(code_point)) {
            point.translate_by(space_width, 0);
            last_code_point = code_point;
            continue;
        }

        auto kerning = font.glyphs_horizontal_kerning(last_code_point, code_point);
        if (kerning != 0.0f)
            point.translate_by(kerning, 0);

        auto it = code_point_iterator; // The callback function will advance the iterator, so create a copy for this lookup.
        auto glyph_width = font.glyph_or_emoji_width(it) + font.glyph_spacing();

        auto glyph_point = point;
        if (include_left_bearing == IncludeLeftBearing::Yes)
            glyph_point += FloatPoint(font.glyph_left_bearing(code_point), 0);

        callback(GlyphPosition {
            .position = glyph_point,
            .glyph_width = glyph_width,
            .it = code_point_iterator });

        point.translate_by(glyph_width, 0);
        last_code_point = code_point;
    }
}

}
