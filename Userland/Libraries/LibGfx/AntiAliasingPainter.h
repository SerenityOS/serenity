/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Painter.h>

namespace Gfx {

class AntiAliasingPainter {
public:
    explicit AntiAliasingPainter(Painter& painter)
        : m_underlying_painter(painter)
    {
    }

    void draw_line(FloatPoint const&, FloatPoint const&, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid, Color alternate_color = Color::Transparent);
    void draw_aliased_line(FloatPoint const&, FloatPoint const&, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid, Color alternate_color = Color::Transparent);
    void fill_path(Path&, Color, Painter::WindingRule rule = Painter::WindingRule::Nonzero);
    void stroke_path(Path const&, Color, float thickness);
    void draw_quadratic_bezier_curve(FloatPoint const& control_point, FloatPoint const&, FloatPoint const&, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid);
    void draw_cubic_bezier_curve(FloatPoint const& control_point_0, FloatPoint const& control_point_1, FloatPoint const&, FloatPoint const&, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid);
    void draw_elliptical_arc(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& center, FloatPoint const& radii, float x_axis_rotation, float theta_1, float theta_delta, Color, float thickness = 1, Painter::LineStyle style = Painter::LineStyle::Solid);

    void translate(float dx, float dy) { m_transform.translate(dx, dy); }
    void translate(FloatPoint const& delta) { m_transform.translate(delta); }

    void draw_circle(IntPoint const& center, int radius, Color);
    void draw_ellipse(IntRect const& a_rect, Color);
    void fill_rect_with_rounded_corners(IntRect const&, Color, int radius);
    void fill_rect_with_rounded_corners(IntRect const&, Color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);

private:
    struct Range {
        int min;
        int max;

        inline bool contains_inclusive(int n) const
        {
            return n >= min && n <= max;
        }
    };
    Range draw_ellipse_part(IntPoint a_rect, int radius_a, int radius_b, Color, bool flip_x_and_y, Optional<Range> x_clip);

    enum class AntiAliasPolicy {
        OnlyEnds,
        Full,
    };
    template<AntiAliasPolicy policy>
    void draw_anti_aliased_line(FloatPoint const&, FloatPoint const&, Color, float thickness, Painter::LineStyle style, Color alternate_color);

    Painter& m_underlying_painter;
    AffineTransform m_transform;
};

}
