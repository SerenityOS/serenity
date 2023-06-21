/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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

}
