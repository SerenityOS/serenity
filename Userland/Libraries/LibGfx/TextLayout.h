/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/Forward.h"
#include "LibGfx/Forward.h"
#include <AK/String.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGfx/Font.h>
#include <LibGfx/Rect.h>
#include <LibGfx/TextElision.h>
#include <LibGfx/TextWrapping.h>

namespace Gfx {

enum class FitWithinRect {
    Yes,
    No
};

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
    TextLayout(Gfx::Font const* font, Utf8View const& text, IntRect const& rect)
        : m_font(font)
        , m_text(text)
        , m_rect(rect)
    {
    }

    Font const& font() const { return *m_font; }
    void set_font(Font const* font) { m_font = font; }

    Utf8View const& text() const { return m_text; }
    void set_text(Utf8View const& text) { m_text = text; }

    IntRect const& rect() const { return m_rect; }
    void set_rect(IntRect const& rect) { m_rect = rect; }

    Vector<String, 32> lines(TextElision elision, TextWrapping wrapping, int line_spacing) const
    {
        return wrap_lines(elision, wrapping, line_spacing, FitWithinRect::Yes);
    }

    IntRect bounding_rect(TextWrapping wrapping, int line_spacing) const;

private:
    Vector<String, 32> wrap_lines(TextElision, TextWrapping, int line_spacing, FitWithinRect) const;
    String elide_text_from_right(Utf8View, bool force_elision) const;

    Font const* m_font;
    Utf8View m_text;
    IntRect m_rect;
};

}
