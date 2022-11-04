/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/FillPainter.h>
#include <LibGfx/Rasterizer.h>
#include <LibGfx/StrokePainter.h>

namespace Gfx {

class PathPainter {
public:
    enum class StrokeKind {
        OpenStroke,
        ClosedStroke,
        NoStroke
    };
    enum class FillKind {
        Filled,
        NotFilled
    };
    PathPainter(Gfx::Bitmap& image);
    void begin_path(StrokeKind stroke_kind, FillKind fill_kind, float thickness);
    void move_to(Point<float> p);
    void line_to(
        Point<float> to);
    void quadratic_bezier_curve_to(
        Point<float> control,
        Point<float> to);
    void cubic_bezier_curve_to(
        Point<float> control1,
        Point<float> control2,
        Point<float> to);
    void elliptical_arc_to(FloatPoint to, FloatPoint center, FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta);
    void end_path(Rasterizer::Paint const& stroke_paint, Rasterizer::Paint const& fill_paint);
    void set_transform(AffineTransform const& transform);
    void set_clip_rect(const IntRect clip_rect);

private:
    enum class Phase { Idle,
        Begun,
        Painting };

    void check_begin();
    void check_end();
    void end();
    void tesslate_cubic_bezier_curve(
        Point<float> from,
        Point<float> control1,
        Point<float> control2,
        Point<float> to,
        int recursion_depth = 0);
    void tesselate_quadratic_bezier_curve(
        Point<float> from,
        Point<float> control,
        Point<float> to,
        int recursion_depth = 0);
    void begin_paint(Point<float> p);
    void edge_to(Point<float> p);

    constexpr static float m_max_tesselation_error = 0.05f;
    bool m_in_path = false;
    StrokePainter m_stroke_painter;
    FillPainter m_fill_painter;
    StrokeKind m_stroke_kind { StrokeKind::NoStroke };
    FillKind m_fill_kind { FillKind::NotFilled };
    float m_thickness = 0;
    Point<float> m_position { 0, 0 };
};

}
