/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Memory.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/PaintStyle.h>
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
    static constexpr int LINE_SPACING = 4;

    explicit Painter(Gfx::Bitmap&);
    ~Painter() = default;

    enum class LineStyle {
        Solid,
        Dotted,
        Dashed,
    };

    enum class ScalingMode {
        NearestFractional,
        NearestNeighbor,
        SmoothPixels,
        BilinearBlend,
        None,
    };

    void clear_rect(IntRect const&, Color);
    void fill_rect(IntRect const&, Color);
    void fill_rect(IntRect const&, PaintStyle const&);
    void fill_rect_with_dither_pattern(IntRect const&, Color, Color);
    void fill_rect_with_checkerboard(IntRect const&, IntSize, Color color_dark, Color color_light);
    void fill_rect_with_gradient(Orientation, IntRect const&, Color gradient_start, Color gradient_end);
    void fill_rect_with_gradient(IntRect const&, Color gradient_start, Color gradient_end);
    void fill_rect_with_linear_gradient(IntRect const&, ReadonlySpan<ColorStop>, float angle, Optional<float> repeat_length = {});
    void fill_rect_with_conic_gradient(IntRect const&, ReadonlySpan<ColorStop>, IntPoint center, float start_angle, Optional<float> repeat_length = {});
    void fill_rect_with_radial_gradient(IntRect const&, ReadonlySpan<ColorStop>, IntPoint center, IntSize size, Optional<float> repeat_length = {});
    void fill_rect_with_rounded_corners(IntRect const&, Color, int radius);
    void fill_rect_with_rounded_corners(IntRect const&, Color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);
    void fill_ellipse(IntRect const&, Color);
    void draw_rect(IntRect const&, Color, bool rough = false);
    void draw_rect_with_thickness(IntRect const&, Color, int thickness);
    void draw_focus_rect(IntRect const&, Color);
    void draw_bitmap(IntPoint, CharacterBitmap const&, Color = Color());
    void draw_bitmap(IntPoint, GlyphBitmap const&, Color = Color());
    void draw_scaled_bitmap(IntRect const& dst_rect, Gfx::Bitmap const&, IntRect const& src_rect, float opacity = 1.0f, ScalingMode = ScalingMode::NearestNeighbor);
    void draw_scaled_bitmap(IntRect const& dst_rect, Gfx::Bitmap const&, FloatRect const& src_rect, float opacity = 1.0f, ScalingMode = ScalingMode::NearestNeighbor);
    void draw_scaled_bitmap_with_transform(IntRect const& dst_rect, Gfx::Bitmap const&, FloatRect const& src_rect, Gfx::AffineTransform const&, float opacity = 1.0f, ScalingMode = ScalingMode::NearestNeighbor);
    void draw_triangle(IntPoint, IntPoint, IntPoint, Color);
    void draw_triangle(IntPoint offset, ReadonlySpan<IntPoint>, Color);
    void draw_ellipse_intersecting(IntRect const&, Color, int thickness = 1);
    void set_pixel(IntPoint, Color, bool blend = false);
    void set_pixel(int x, int y, Color color, bool blend = false) { set_pixel({ x, y }, color, blend); }
    Optional<Color> get_pixel(IntPoint);
    ErrorOr<NonnullRefPtr<Bitmap>> get_region_bitmap(IntRect const&, BitmapFormat format, Optional<IntRect&> actual_region = {});
    void draw_line(IntPoint, IntPoint, Color, int thickness = 1, LineStyle style = LineStyle::Solid, Color alternate_color = Color::Transparent);
    void draw_triangle_wave(IntPoint, IntPoint, Color color, int amplitude, int thickness = 1);
    void draw_quadratic_bezier_curve(IntPoint control_point, IntPoint, IntPoint, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void draw_cubic_bezier_curve(IntPoint control_point_0, IntPoint control_point_1, IntPoint, IntPoint, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void draw_elliptical_arc(IntPoint p1, IntPoint p2, IntPoint center, FloatSize radii, float x_axis_rotation, float theta_1, float theta_delta, Color, int thickness = 1, LineStyle style = LineStyle::Solid);
    void blit(IntPoint, Gfx::Bitmap const&, IntRect const& src_rect, float opacity = 1.0f, bool apply_alpha = true);
    void blit_dimmed(IntPoint, Gfx::Bitmap const&, IntRect const& src_rect);
    void blit_brightened(IntPoint, Gfx::Bitmap const&, IntRect const& src_rect);
    void blit_filtered(IntPoint, Gfx::Bitmap const&, IntRect const& src_rect, Function<Color(Color)>);
    void draw_tiled_bitmap(IntRect const& dst_rect, Gfx::Bitmap const&);
    void blit_offset(IntPoint, Gfx::Bitmap const&, IntRect const& src_rect, IntPoint);
    void blit_disabled(IntPoint, Gfx::Bitmap const&, IntRect const&, Palette const&);
    void blit_tiled(IntRect const&, Gfx::Bitmap const&, IntRect const& src_rect);
    void draw_text(FloatRect const&, StringView, Font const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(FloatRect const&, StringView, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(FloatRect const&, Utf32View const&, Font const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(FloatRect const&, Utf32View const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)>, FloatRect const&, StringView, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)>, FloatRect const&, Utf8View const&, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)>, FloatRect const&, Utf32View const&, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, StringView, Font const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, StringView, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, Utf32View const&, Font const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(IntRect const&, Utf32View const&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)>, IntRect const&, StringView, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)>, IntRect const&, Utf8View const&, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)>, IntRect const&, Utf32View const&, Font const&, TextAlignment = TextAlignment::TopLeft, TextElision = TextElision::None, TextWrapping = TextWrapping::DontWrap);
    void draw_ui_text(Gfx::IntRect const&, StringView, Gfx::Font const&, TextAlignment, Gfx::Color);
    void draw_glyph(IntPoint, u32, Color);
    void draw_glyph(IntPoint, u32, Font const&, Color);
    void draw_emoji(IntPoint, Gfx::Bitmap const&, Font const&);
    void draw_glyph_or_emoji(IntPoint, u32, Font const&, Color);
    void draw_glyph_or_emoji(IntPoint, Utf8CodePointIterator&, Font const&, Color);
    void draw_glyph(FloatPoint, u32, Color);
    void draw_glyph(FloatPoint, u32, Font const&, Color);
    void draw_glyph_or_emoji(FloatPoint, u32, Font const&, Color);
    void draw_glyph_or_emoji(FloatPoint, Utf8CodePointIterator&, Font const&, Color);
    void draw_circle_arc_intersecting(IntRect const&, IntPoint, int radius, Color, int thickness);

    // Streamlined text drawing routine that does no wrapping/elision/alignment.
    void draw_text_run(IntPoint baseline_start, Utf8View const&, Font const&, Color);
    void draw_text_run(FloatPoint baseline_start, Utf8View const&, Font const&, Color);

    enum class CornerOrientation {
        TopLeft,
        TopRight,
        BottomRight,
        BottomLeft
    };
    void fill_rounded_corner(IntRect const&, int radius, Color, CornerOrientation);

    static void for_each_line_segment_on_bezier_curve(FloatPoint control_point, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>&);
    static void for_each_line_segment_on_bezier_curve(FloatPoint control_point, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>&&);

    static void for_each_line_segment_on_cubic_bezier_curve(FloatPoint control_point_0, FloatPoint control_point_1, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>&);
    static void for_each_line_segment_on_cubic_bezier_curve(FloatPoint control_point_0, FloatPoint control_point_1, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>&&);

    static void for_each_line_segment_on_elliptical_arc(FloatPoint p1, FloatPoint p2, FloatPoint center, FloatSize radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint, FloatPoint)>&);
    static void for_each_line_segment_on_elliptical_arc(FloatPoint p1, FloatPoint p2, FloatPoint center, FloatSize radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint, FloatPoint)>&&);

    void stroke_path(Path const&, Color, int thickness);

    enum class WindingRule {
        Nonzero,
        EvenOdd,
    };
    void fill_path(Path const&, Color, WindingRule rule = WindingRule::Nonzero);
    void fill_path(Path const&, PaintStyle const& paint_style, WindingRule rule = WindingRule::Nonzero);

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
    void translate(IntPoint delta) { state().translation.translate_by(delta); }

    IntPoint translation() const { return state().translation; }

    Gfx::Bitmap* target() { return m_target.ptr(); }

    void save() { m_state_stack.append(m_state_stack.last()); }
    void restore()
    {
        VERIFY(m_state_stack.size() > 1);
        m_state_stack.take_last();
    }

    IntRect clip_rect() const { return state().clip_rect; }

    int scale() const { return state().scale; }

    template<typename T, typename TColorOrFunction>
    ALWAYS_INLINE void draw_scanline_for_fill_path(int y, T x_start, T x_end, TColorOrFunction color)
    {
        // Note: This is really an internal function for FillPathImplementation.h to use.
        // This allows fill path to clip more of the pixels and reduce the number of clipping checks
        // to the number of scanlines (and allows for a fast fill).

        // Fill path should scale the scanlines before calling this.
        VERIFY(scale() == 1);

        constexpr bool is_floating_point = IsSameIgnoringCV<T, int>;
        constexpr bool has_constant_color = IsSameIgnoringCV<TColorOrFunction, Color>;

        int x1 = 0;
        int x2 = 0;
        u8 left_subpixel_alpha = 0;
        u8 right_subpixel_alpha = 0;
        if constexpr (is_floating_point) {
            x1 = ceilf(x_start);
            x2 = floorf(x_end);
            left_subpixel_alpha = (x1 - x_start) * 255;
            right_subpixel_alpha = (x_end - x2) * 255;
            x1 -= left_subpixel_alpha > 0;
            x2 += right_subpixel_alpha > 0;
        } else {
            x1 = x_start;
            x2 = x_end;
        }

        IntRect scanline(x1, y, x2 - x1, 1);
        scanline = scanline.translated(translation());
        auto clipped = scanline.intersected(clip_rect());
        if (clipped.is_empty())
            return;

        auto get_color = [&](int offset) {
            if constexpr (has_constant_color) {
                return color;
            } else {
                return color(offset);
            }
        };

        if constexpr (is_floating_point) {
            // Paint left and right subpixels (then remove them from the scanline).
            auto get_color_with_alpha = [&](int offset, u8 alpha) {
                auto color_at_offset = get_color(offset);
                u8 color_alpha = (alpha * color_at_offset.alpha()) / 255;
                return color_at_offset.with_alpha(color_alpha);
            };
            if (clipped.left() == scanline.left() && left_subpixel_alpha)
                set_physical_pixel(clipped.top_left(), get_color_with_alpha(0, left_subpixel_alpha), true);
            if (clipped.right() == scanline.right() && right_subpixel_alpha)
                set_physical_pixel(clipped.top_right(), get_color_with_alpha(scanline.width(), right_subpixel_alpha), true);
            clipped.shrink(0, right_subpixel_alpha > 0, 0, left_subpixel_alpha > 0);
        }

        if constexpr (has_constant_color) {
            if (color.alpha() == 255) {
                // Speedy path: Constant color and no alpha blending.
                fast_u32_fill(m_target->scanline(clipped.y()) + clipped.x(), color.value(), clipped.width());
                return;
            }
        }

        for (int x = clipped.x(); x <= clipped.right(); x++) {
            set_physical_pixel({ x, clipped.y() }, get_color(x - scanline.x()), true);
        }
    }

protected:
    friend GradientLine;

    IntRect to_physical(IntRect const& r) const { return r.translated(translation()) * scale(); }
    IntPoint to_physical(IntPoint p) const { return p.translated(translation()) * scale(); }
    void set_physical_pixel_with_draw_op(u32& pixel, Color);
    void fill_physical_scanline_with_draw_op(int y, int x, int width, Color color);
    void fill_rect_with_draw_op(IntRect const&, Color);
    void blit_with_opacity(IntPoint, Gfx::Bitmap const&, IntRect const& src_rect, float opacity, bool apply_alpha = true);
    void draw_physical_pixel(IntPoint, Color, int thickness = 1);
    void set_physical_pixel(IntPoint, Color color, bool blend);

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
    void do_draw_text(FloatRect const&, Utf8View const& text, Font const&, TextAlignment, TextElision, TextWrapping, DrawGlyphFunction);
};

class PainterStateSaver {
public:
    explicit PainterStateSaver(Painter&);
    ~PainterStateSaver();

private:
    Painter& m_painter;
};

DeprecatedString parse_ampersand_string(StringView, Optional<size_t>* underline_offset = nullptr);

}
