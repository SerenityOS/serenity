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

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/TextAlignment.h>
#include <LibGfx/TextElision.h>

namespace Gfx {

class Painter {
public:
    explicit Painter(Gfx::Bitmap&);
    ~Painter();

    enum class LineStyle {
        Solid,
        Dotted,
        Dashed,
    };

    void clear_rect(const IntRect&, Color);
    void fill_rect(const IntRect&, Color);
    void fill_rect_with_dither_pattern(const IntRect&, Color, Color);
    void fill_rect_with_checkerboard(const IntRect&, const IntSize&, Color color_dark, Color color_light);
    void fill_rect_with_gradient(Orientation, const IntRect&, Color gradient_start, Color gradient_end);
    void fill_rect_with_gradient(const IntRect&, Color gradient_start, Color gradient_end);
    void fill_ellipse(const IntRect&, Color);
    void draw_rect(const IntRect&, Color, bool rough = false);
    void draw_bitmap(const IntPoint&, const CharacterBitmap&, Color = Color());
    void draw_bitmap(const IntPoint&, const GlyphBitmap&, Color = Color());
    void draw_scaled_bitmap(const IntRect& dst_rect, const Gfx::Bitmap&, const IntRect& src_rect, float opacity = 1.0f);
    void draw_triangle(const IntPoint&, const IntPoint&, const IntPoint&, Color);
    void draw_ellipse_intersecting(const IntRect&, Color, int thickness = 1);
    void set_pixel(const IntPoint&, Color);
    void set_pixel(int x, int y, Color color) { set_pixel({ x, y }, color); }
    void draw_line(const IntPoint&, const IntPoint&, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void draw_quadratic_bezier_curve(const IntPoint& control_point, const IntPoint&, const IntPoint&, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void draw_elliptical_arc(const IntPoint& p1, const IntPoint& p2, const IntPoint& center, const FloatPoint& radii, float x_axis_rotation, float theta_1, float theta_delta, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void blit(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect, float opacity = 1.0f);
    void blit_dimmed(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect);
    void blit_brightened(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect);
    void blit_filtered(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect, Function<Color(Color)>);
    void draw_tiled_bitmap(const IntRect& dst_rect, const Gfx::Bitmap&);
    void blit_offset(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect, const IntPoint&);
    void blit_scaled(const IntRect&, const Gfx::Bitmap&, const IntRect&, float, float);
    void draw_text(const IntRect&, const StringView&, const Font&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_text(const IntRect&, const StringView&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_text(const IntRect&, const Utf32View&, const Font&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_text(const IntRect&, const Utf32View&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_glyph(const IntPoint&, u32, Color);
    void draw_glyph(const IntPoint&, u32, const Font&, Color);
    void draw_emoji(const IntPoint&, const Gfx::Bitmap&, const Font&);
    void draw_glyph_or_emoji(const IntPoint&, u32 code_point, const Font&, Color);

    static void for_each_line_segment_on_bezier_curve(const FloatPoint& control_point, const FloatPoint& p1, const FloatPoint& p2, Function<void(const FloatPoint&, const FloatPoint&)>&);
    static void for_each_line_segment_on_bezier_curve(const FloatPoint& control_point, const FloatPoint& p1, const FloatPoint& p2, Function<void(const FloatPoint&, const FloatPoint&)>&&);

    static void for_each_line_segment_on_elliptical_arc(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(const FloatPoint&, const FloatPoint&)>&);
    static void for_each_line_segment_on_elliptical_arc(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(const FloatPoint&, const FloatPoint&)>&&);

    void stroke_path(const Path&, Color, int thickness);

    enum class WindingRule {
        Nonzero,
        EvenOdd,
    };
    void fill_path(Path&, Color, WindingRule rule = WindingRule::Nonzero);

    const Font& font() const { return *state().font; }
    void set_font(const Font& font) { state().font = &font; }

    enum class DrawOp {
        Copy,
        Xor,
        Invert
    };
    void set_draw_op(DrawOp op) { state().draw_op = op; }
    DrawOp draw_op() const { return state().draw_op; }

    void add_clip_rect(const IntRect& rect);
    void clear_clip_rect();
    IntRect clip_rect() const { return state().clip_rect; }

    void translate(int dx, int dy) { state().translation.move_by(dx, dy); }
    void translate(const IntPoint& delta) { state().translation.move_by(delta); }

    IntPoint translation() const { return state().translation; }

    Gfx::Bitmap* target() { return m_target.ptr(); }

    void save() { m_state_stack.append(m_state_stack.last()); }
    void restore()
    {
        ASSERT(m_state_stack.size() > 1);
        m_state_stack.take_last();
    }

protected:
    void set_pixel_with_draw_op(u32& pixel, const Color&);
    void fill_scanline_with_draw_op(int y, int x, int width, const Color& color);
    void fill_rect_with_draw_op(const IntRect&, Color);
    void blit_with_alpha(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect);
    void blit_with_opacity(const IntPoint&, const Gfx::Bitmap&, const IntRect& src_rect, float opacity);
    void draw_pixel(const IntPoint&, Color, int thickness = 1);

    void draw_text_line(const IntRect&, const Utf8View&, const Font&, TextAlignment, Color, TextElision);
    void draw_text_line(const IntRect&, const Utf32View&, const Font&, TextAlignment, Color, TextElision);

    struct State {
        const Font* font;
        IntPoint translation;
        IntRect clip_rect;
        DrawOp draw_op;
    };

    State& state() { return m_state_stack.last(); }
    const State& state() const { return m_state_stack.last(); }

    IntRect m_clip_origin;
    NonnullRefPtr<Gfx::Bitmap> m_target;
    Vector<State, 4> m_state_stack;
};

class PainterStateSaver {
public:
    explicit PainterStateSaver(Painter&);
    ~PainterStateSaver();

private:
    Painter& m_painter;
};

}
