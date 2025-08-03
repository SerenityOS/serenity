/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibGfx/CornerRadius.h>
#include <LibGfx/EdgeFlagPathRasterizer.h>
#include <LibGfx/Forward.h>
#include <LibGfx/LineStyle.h>
#include <LibGfx/PaintStyle.h>
#include <LibGfx/Path.h>
#include <LibGfx/Quad.h>
#include <LibGfx/WindingRule.h>

namespace Gfx {

class AntiAliasingPainter {
public:
    explicit AntiAliasingPainter(Painter& painter)
        : m_underlying_painter(painter)
    {
    }

    void draw_line(IntPoint, IntPoint, Color, float thickness = 1, LineStyle style = LineStyle::Solid, Color alternate_color = Color::Transparent);
    void draw_line(FloatPoint, FloatPoint, Color, float thickness = 1, LineStyle style = LineStyle::Solid, Color alternate_color = Color::Transparent);
    void draw_line(FloatLine line, Color color, float thickness = 1, LineStyle style = LineStyle::Solid, Color alternate_color = Color::Transparent)
    {
        draw_line(line.a(), line.b(), color, thickness, style, alternate_color);
    }

    template<typename SampleMode = Sample32xAA>
    void fill_path(Path const& path, Color color, WindingRule winding_rule = WindingRule::Nonzero)
    {
        EdgeFlagPathRasterizer<SampleMode> rasterizer(path_bounds(path));
        rasterizer.fill(m_underlying_painter, path, color, winding_rule, m_transform.translation());
    }

    template<typename SampleMode = Sample32xAA>
    void fill_path(Path const& path, PaintStyle const& paint_style, float opacity = 1.0f, WindingRule winding_rule = WindingRule::Nonzero)
    {
        EdgeFlagPathRasterizer<SampleMode> rasterizer(path_bounds(path));
        rasterizer.fill(m_underlying_painter, path, paint_style, opacity, winding_rule, m_transform.translation());
    }

    void stroke_path(Path const&, Color, Path::StrokeStyle const& stroke_style);
    void stroke_path(Path const&, PaintStyle const& paint_style, Path::StrokeStyle const&, float opacity = 1.0f);

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

    Range draw_ellipse_part(IntPoint a_rect, int radius_a, int radius_b, Color alternate_color, bool flip_x_and_y, Optional<Range> x_clip, BlendMode blend_mode);

    Painter& m_underlying_painter;
    AffineTransform m_transform;
};

}
