/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibGfx/Quad.h>

namespace Gfx {

class AntiAliasingPainter {
public:
    explicit AntiAliasingPainter(Painter& painter)
        : m_underlying_painter(painter)
    {
    }

    enum class LineLengthMode {
        // E.g. A line from 0,1 -> 2,1 is 3px long
        PointToPoint,
        // E.g. A line from 0,1 -> 2,1 is 2px long
        Distance
    };

    void draw_line(IntPoint, IntPoint, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid, Color alternate_color = Color::Transparent, LineLengthMode line_length_mode = LineLengthMode::PointToPoint);
    void draw_line(FloatPoint, FloatPoint, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid, Color alternate_color = Color::Transparent, LineLengthMode line_length_mode = LineLengthMode::PointToPoint);
    void draw_line_for_path(FloatPoint, FloatPoint, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid, Color alternate_color = Color::Transparent, LineLengthMode line_length_mode = LineLengthMode::PointToPoint);

    void fill_path(Path const&, Color, Painter::WindingRule rule = Painter::WindingRule::Nonzero);
    void fill_path(Path const&, PaintStyle const& paint_style, Painter::WindingRule rule = Painter::WindingRule::Nonzero);

    void stroke_path(Path const&, Color, float thickness);
    void draw_quadratic_bezier_curve(FloatPoint control_point, FloatPoint, FloatPoint, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid);
    void draw_cubic_bezier_curve(FloatPoint control_point_0, FloatPoint control_point_1, FloatPoint, FloatPoint, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid);
    void draw_elliptical_arc(FloatPoint p1, FloatPoint p2, FloatPoint center, FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid);

    void translate(float dx, float dy) { m_transform.translate(dx, dy); }
    void translate(FloatPoint delta) { m_transform.translate(delta); }

    void draw_ellipse(IntRect const& a_rect, Color, int thickness);

    enum class BlendMode {
        Normal,
        AlphaSubtract
    };

    void fill_rect(FloatRect const&, Color);

    void fill_circle(IntPoint center, int radius, Color, BlendMode blend_mode = BlendMode::Normal);
    void fill_ellipse(IntRect const& a_rect, Color, BlendMode blend_mode = BlendMode::Normal);

    void fill_rect_with_rounded_corners(IntRect const&, Color, int radius);
    void fill_rect_with_rounded_corners(IntRect const&, Color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);

    struct CornerRadius {
        int horizontal_radius;
        int vertical_radius;

        inline operator bool() const
        {
            return horizontal_radius > 0 && vertical_radius > 0;
        }

        Gfx::IntRect as_rect() const
        {
            return { 0, 0, horizontal_radius, vertical_radius };
        }
    };

    void fill_rect_with_rounded_corners(IntRect const&, Color, CornerRadius top_left, CornerRadius top_right, CornerRadius bottom_right, CornerRadius bottom_left, BlendMode blend_mode = BlendMode::Normal);

    Gfx::Painter& underlying_painter() { return m_underlying_painter; }

private:
    struct Range {
        int min;
        int max;

        inline bool contains_inclusive(int n) const
        {
            return n >= min && n <= max;
        }
    };

    Range draw_ellipse_part(IntPoint a_rect, int radius_a, int radius_b, Color, bool flip_x_and_y, Optional<Range> x_clip, BlendMode blend_mode);

    void draw_dotted_line(IntPoint, IntPoint, Gfx::Color, int thickness);

    enum class FixmeEnableHacksForBetterPathPainting {
        Yes,
        No,
    };

    template<FixmeEnableHacksForBetterPathPainting path_hacks>
    void draw_anti_aliased_line(FloatPoint, FloatPoint, Color, float thickness, Painter::LineStyle style, Color alternate_color, LineLengthMode line_length_mode = LineLengthMode::PointToPoint);

    void stroke_segment_intersection(FloatPoint current_line_a, FloatPoint current_line_b, FloatLine const& previous_line, Color, float thickness);

    Painter& m_underlying_painter;
    AffineTransform m_transform;
};

}
