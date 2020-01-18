/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibDraw/TextAlignment.h>
#include <LibDraw/TextElision.h>

class CharacterBitmap;
class GlyphBitmap;
class GraphicsBitmap;
class Font;
class Emoji;

class Painter {
public:
    explicit Painter(GraphicsBitmap&);
    ~Painter();
    void clear_rect(const Rect&, Color);
    void fill_rect(const Rect&, Color);
    void fill_rect_with_gradient(const Rect&, Color gradient_start, Color gradient_end);
    void draw_rect(const Rect&, Color, bool rough = false);
    void draw_bitmap(const Point&, const CharacterBitmap&, Color = Color());
    void draw_bitmap(const Point&, const GlyphBitmap&, Color = Color());
    void draw_ellipse_intersecting(const Rect&, Color, int thickness = 1);
    void set_pixel(const Point&, Color);
    void draw_line(const Point&, const Point&, Color, int thickness = 1, bool dotted = false);
    void draw_scaled_bitmap(const Rect& dst_rect, const GraphicsBitmap&, const Rect& src_rect);
    void blit(const Point&, const GraphicsBitmap&, const Rect& src_rect, float opacity = 1.0f);
    void blit_dimmed(const Point&, const GraphicsBitmap&, const Rect& src_rect);
    void draw_tiled_bitmap(const Rect& dst_rect, const GraphicsBitmap&);
    void blit_offset(const Point&, const GraphicsBitmap&, const Rect& src_rect, const Point&);
    void blit_scaled(const Rect&, const GraphicsBitmap&, const Rect&, float, float);
    void draw_text(const Rect&, const StringView&, const Font&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_text(const Rect&, const StringView&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_glyph(const Point&, char, Color);
    void draw_glyph(const Point&, char, const Font&, Color);
    void draw_emoji(const Point&, const GraphicsBitmap&, const Font&);
    void draw_glyph_or_emoji(const Point&, u32 codepoint, const Font&, Color);

    const Font& font() const { return *state().font; }
    void set_font(const Font& font) { state().font = &font; }

    enum class DrawOp {
        Copy,
        Xor
    };
    void set_draw_op(DrawOp op) { state().draw_op = op; }
    DrawOp draw_op() const { return state().draw_op; }

    void add_clip_rect(const Rect& rect);
    void clear_clip_rect();
    Rect clip_rect() const { return state().clip_rect; }

    void translate(int dx, int dy) { state().translation.move_by(dx, dy); }
    void translate(const Point& delta) { state().translation.move_by(delta); }

    Point translation() const { return state().translation; }

    GraphicsBitmap* target() { return m_target.ptr(); }

    void save() { m_state_stack.append(m_state_stack.last()); }
    void restore()
    {
        ASSERT(m_state_stack.size() > 1);
        m_state_stack.take_last();
    }

protected:
    void set_pixel_with_draw_op(u32& pixel, const Color&);
    void fill_rect_with_draw_op(const Rect&, Color);
    void blit_with_alpha(const Point&, const GraphicsBitmap&, const Rect& src_rect);
    void blit_with_opacity(const Point&, const GraphicsBitmap&, const Rect& src_rect, float opacity);
    void draw_pixel(const Point&, Color, int thickness = 1);

    void draw_text_line(const Rect&, const Utf8View&, const Font&, TextAlignment, Color, TextElision);

    struct State {
        const Font* font;
        Point translation;
        Rect clip_rect;
        DrawOp draw_op;
    };

    State& state() { return m_state_stack.last(); }
    const State& state() const { return m_state_stack.last(); }

    Rect m_clip_origin;
    NonnullRefPtr<GraphicsBitmap> m_target;
    Vector<State, 4> m_state_stack;
};

class PainterStateSaver {
public:
    explicit PainterStateSaver(Painter&);
    ~PainterStateSaver();

private:
    Painter& m_painter;
};
