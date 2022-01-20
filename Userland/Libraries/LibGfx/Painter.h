/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/TextAlignment.h>
#include <LibGfx/TextDirection.h>
#include <LibGfx/TextElision.h>
#include <LibGfx/TextWrapping.h>

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

    enum class ScalingMode {
        NearestNeighbor,
        BilinearBlend,
    };

    void clear_rect(IntRect const&, Color);
    void fill_rect(IntRect const&, Color);
    void fill_rect_with_dither_pattern(IntRect const&, Color, Color);
    void fill_rect_with_checkerboard(IntRect const&, IntSize const&, Color color_dark, Color color_light);
    void fill_rect_with_gradient(Orientation, IntRect const&, Color gradient_start, Color gradient_end);
    void fill_rect_with_gradient(IntRect const&, Color gradient_start, Color gradient_end);
    void fill_rect_with_rounded_corners(IntRect const&, Color, int radius);
    void fill_rect_with_rounded_corners(IntRect const&, Color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);
    void fill_ellipse(IntRect const&, Color);
    void draw_rect(IntRect const&, Color, bool rough = false);
    void draw_rect_with_thickness(IntRect const&, Color, int thickness);
    void draw_focus_rect(IntRect const&, Color);
    void draw_bitmap(IntPoint const&, CharacterBitmap const&, Color = Color());
    void draw_bitmap(IntPoint const&, GlyphBitmap const&, Color = Color());
    void draw_scaled_bitmap(IntRect const& dst_rect, Gfx::Bitmap const&, IntRect const& src_rect, float opacity = 1.0f, ScalingMode = ScalingMode::NearestNeighbor);
    void draw_scaled_bitmap(IntRect const& dst_rect, Gfx::Bitmap const&, FloatRect const& src_rect, float opacity = 1.0f, ScalingMode = ScalingMode::NearestNeighbor);
    void draw_triangle(IntPoint const&, IntPoint const&, IntPoint const&, Color);
    void draw_ellipse_intersecting(IntRect const&, Color, int thickness = 1);
    void set_pixel(IntPoint const&, Color);
    void set_pixel(int x, int y, Color color) { set_pixel({ x, y }, color); }
    void draw_line(IntPoint const&, IntPoint const&, Color, int thickness = 1, LineStyle style = LineStyle::Solid, Color alternate_color = Color::Transparent);
    void draw_triangle_wave(IntPoint const&, IntPoint const&, Color color, int amplitude, int thickness = 1);
    void draw_quadratic_bezier_curve(IntPoint const& control_point, IntPoint const&, IntPoint const&, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void draw_cubic_bezier_curve(IntPoint const& control_point_0, IntPoint const& control_point_1, IntPoint const&, IntPoint const&, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void draw_elliptical_arc(IntPoint const& p1, IntPoint const& p2, IntPoint const& center, FloatPoint const& radii, float x_axis_rotation, float theta_1, float theta_delta, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void blit(IntPoint const&, Gfx::Bitmap const&, IntRect const& src_rect, float opacity = 1.0f, bool apply_alpha = true);
    void blit_dimmed(IntPoint const&, Gfx::Bitmap const&, IntRect const& src_rect);
    void blit_brightened(IntPoint const&, Gfx::Bitmap const&, IntRect const& src_rect);
    void blit_filtered(IntPoint const&, Gfx::Bitmap const&, IntRect const& src_rect, Function<Color(Color)>);
    void draw_tiled_bitmap(IntRect const& dst_rect, Gfx::Bitmap const&);
    void blit_offset(IntPoint const&, Gfx::Bitmap const&, IntRect const& src_rect, IntPoint const&);
    void blit_disabled(IntPoint const&, Gfx::Bitmap const&, IntRect const&, Palette const&);
    void blit_tiled(IntRect const&, Gfx::Bitmap const&, IntRect const& src_rect);
    void draw_text(IntRect const&, StringView, Font const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, StringView, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, Utf32View const&, Font const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, Utf32View const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(IntRect const&, u32)>, IntRect const&, StringView, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(IntRect const&, u32)>, IntRect const&, Utf8View const&, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(IntRect const&, u32)>, IntRect const&, Utf32View const&, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_ui_text(Gfx::IntRect const&, StringView, Gfx::Font const&, TextAlignment, Gfx::Color);
    void draw_glyph(IntPoint const&, u32, Color);
    void draw_glyph(IntPoint const&, u32, Font const&, Color);
    void draw_emoji(IntPoint const&, Gfx::Bitmap const&, Font const&);
    void draw_glyph_or_emoji(IntPoint const&, u32 code_point, Font const&, Color);
    void draw_circle_arc_intersecting(IntRect const&, IntPoint const&, int radius, Color, int thickness);

    enum class CornerOrientation {
        TopLeft,
        TopRight,
        BottomRight,
        BottomLeft
    };
    void fill_rounded_corner(IntRect const&, int radius, Color, CornerOrientation);

    static void for_each_line_segment_on_bezier_curve(FloatPoint const& control_point, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>&);
    static void for_each_line_segment_on_bezier_curve(FloatPoint const& control_point, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>&&);

    static void for_each_line_segment_on_cubic_bezier_curve(FloatPoint const& control_point_0, FloatPoint const& control_point_1, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>&);
    static void for_each_line_segment_on_cubic_bezier_curve(FloatPoint const& control_point_0, FloatPoint const& control_point_1, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>&&);

    static void for_each_line_segment_on_elliptical_arc(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& center, FloatPoint const radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint const&, FloatPoint const&)>&);
    static void for_each_line_segment_on_elliptical_arc(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& center, FloatPoint const radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint const&, FloatPoint const&)>&&);

    void stroke_path(Path const&, Color, int thickness);

    enum class WindingRule {
        Nonzero,
        EvenOdd,
    };
    void fill_path(Path const&, Color, WindingRule rule = WindingRule::Nonzero);

    Font const& font() const
    {
        if (!state().font)
            return FontDatabase::default_font();
        return *state().font;
    }
    void set_font(Font const& font) { state().font = &font; }

    enum class DrawOp {
        Copy,
        Xor,
        Invert
    };
    void set_draw_op(DrawOp op) { state().draw_op = op; }
    DrawOp draw_op() const { return state().draw_op; }

    void add_clip_rect(IntRect const& rect);
    void clear_clip_rect();

    void translate(int dx, int dy) { translate({ dx, dy }); }
    void translate(IntPoint const& delta) { state().translation.translate_by(delta); }

    Gfx::Bitmap* target() { return m_target.ptr(); }

    void save() { m_state_stack.append(m_state_stack.last()); }
    void restore()
    {
        VERIFY(m_state_stack.size() > 1);
        m_state_stack.take_last();
    }

    IntRect clip_rect() const { return state().clip_rect; }

protected:
    IntPoint translation() const { return state().translation; }
    IntRect to_physical(IntRect const& r) const { return r.translated(translation()) * scale(); }
    IntPoint to_physical(IntPoint const& p) const { return p.translated(translation()) * scale(); }
    int scale() const { return state().scale; }
    void set_physical_pixel_with_draw_op(u32& pixel, Color const&);
    void fill_physical_scanline_with_draw_op(int y, int x, int width, Color const& color);
    void fill_rect_with_draw_op(IntRect const&, Color);
    void blit_with_opacity(IntPoint const&, Gfx::Bitmap const&, IntRect const& src_rect, float opacity, bool apply_alpha = true);
    void draw_physical_pixel(IntPoint const&, Color, int thickness = 1);

    struct State {
        Font const* font;
        IntPoint translation;
        int scale = 1;
        IntRect clip_rect;
        DrawOp draw_op;
    };

    State& state() { return m_state_stack.last(); }
    State const& state() const { return m_state_stack.last(); }

    void fill_physical_rect(IntRect const&, Color);

    IntRect m_clip_origin;
    NonnullRefPtr<Gfx::Bitmap> m_target;
    Vector<State, 4> m_state_stack;

private:
    Vector<DirectionalRun> split_text_into_directional_runs(Utf8View const&, TextDirection initial_direction);
    bool text_contains_bidirectional_text(Utf8View const&, TextDirection);
    template<typename DrawGlyphFunction>
    void do_draw_text(IntRect const&, Utf8View const& text, Font const&, TextAlignment, TextElision, TextWrapping, DrawGlyphFunction);
};

class PainterStateSaver {
public:
    explicit PainterStateSaver(Painter&);
    ~PainterStateSaver();

private:
    Painter& m_painter;
};

String parse_ampersand_string(StringView, Optional<size_t>* underline_offset = nullptr);

}
