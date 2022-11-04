/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibGfx/PathPainter.h>

namespace Gfx {

PathPainter::PathPainter(Gfx::Bitmap& image)
    : m_stroke_painter { image }
    , m_fill_painter { image }
{
}

void PathPainter::check_begin()
{
    if (!m_in_path) {
        begin_paint(m_position);
        m_in_path = true;
    }
}

void PathPainter::check_end()
{
    if (m_in_path) {
        end();
        m_in_path = false;
    }
}

void PathPainter::begin_path(StrokeKind stroke_kind, FillKind fill_kind, float thickness)
{
    m_stroke_kind = stroke_kind;
    m_fill_kind = fill_kind;
    m_thickness = thickness;
}

void PathPainter::move_to(Point<float> p)
{
    check_end();
    m_position = p;
}

void PathPainter::line_to(
    Point<float> to)
{
    check_begin();
    edge_to(to);
    m_position = to;
}

void PathPainter::quadratic_bezier_curve_to(
    Point<float> control,
    Point<float> to)
{
    check_begin();
    tesselate_quadratic_bezier_curve(m_position, control, to);
    m_position = to;
}

void PathPainter::cubic_bezier_curve_to(
    Point<float> control1,
    Point<float> control2,
    Point<float> to)
{
    check_begin();
    tesslate_cubic_bezier_curve(m_position, control1, control2, to);
    m_position = to;
}

void PathPainter::elliptical_arc_to(FloatPoint to, FloatPoint center, FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta)
{
    check_begin();
    Painter::for_each_line_segment_on_elliptical_arc(
        m_position,
        to,
        center,
        radii,
        x_axis_rotation,
        theta_1,
        theta_delta,
        [&](FloatPoint /*from*/, FloatPoint to) {
            edge_to(to);
        });
}

void PathPainter::end()
{
    if (m_fill_kind == FillKind::Filled)
        m_fill_painter.end();
    switch (m_stroke_kind) {
    case StrokeKind::OpenStroke:
    case StrokeKind::ClosedStroke:
        m_stroke_painter.end();
        break;
    case StrokeKind::NoStroke:
        break;
    }
    m_in_path = false;
}

void PathPainter::end_path(Rasterizer::Paint const& stroke_paint, Rasterizer::Paint const& fill_paint)
{
    check_end();
    switch (m_stroke_kind) {
    case StrokeKind::OpenStroke:
    case StrokeKind::ClosedStroke:
        m_stroke_painter.end_path(stroke_paint);
        break;
    case StrokeKind::NoStroke:
        break;
    }
    if (m_fill_kind == FillKind::Filled)
        m_fill_painter.end_path(fill_paint);
}

void PathPainter::tesslate_cubic_bezier_curve(
    Point<float> from,
    Point<float> control1,
    Point<float> control2,
    Point<float> to,
    int recursion_depth)
{
    if (recursion_depth > 10 || Painter::can_approximate_cubic_bezier_curve(from, to, control1, control2, m_max_tesselation_error)) {
        edge_to(to);
    } else {
        Array level_1_midpoints {
            (from + control1) / 2,
            (control1 + control2) / 2,
            (control2 + to) / 2,
        };
        Array level_2_midpoints {
            (level_1_midpoints[0] + level_1_midpoints[1]) / 2,
            (level_1_midpoints[1] + level_1_midpoints[2]) / 2,
        };
        auto level_3_midpoint = (level_2_midpoints[0] + level_2_midpoints[1]) / 2;
        tesslate_cubic_bezier_curve(from, level_1_midpoints[0], level_2_midpoints[0], level_3_midpoint, recursion_depth + 1);
        tesslate_cubic_bezier_curve(level_3_midpoint, level_2_midpoints[1], level_1_midpoints[2], to, recursion_depth + 1);
    }
}

void PathPainter::tesselate_quadratic_bezier_curve(
    Point<float> from,
    Point<float> control,
    Point<float> to,
    int recursion_depth)
{
    if (recursion_depth > 10 || Painter::can_approximate_bezier_curve(from, to, control, m_max_tesselation_error)) {
        edge_to(to);
    } else {
        auto po1_midpoint = control + from;
        po1_midpoint /= 2;
        auto po2_midpoint = control + to;
        po2_midpoint /= 2;
        auto new_segment = po1_midpoint + po2_midpoint;
        new_segment /= 2;
        tesselate_quadratic_bezier_curve(from, po1_midpoint, new_segment, recursion_depth + 1);
        tesselate_quadratic_bezier_curve(new_segment, po2_midpoint, to, recursion_depth + 1);
    }
}

void PathPainter::begin_paint(Point<float> p)
{
    switch (m_stroke_kind) {
    case StrokeKind::OpenStroke:
        m_stroke_painter.begin(p, false, m_thickness);
        break;
    case StrokeKind::ClosedStroke:
        m_stroke_painter.begin(p, true, m_thickness);
        break;
    case StrokeKind::NoStroke:
        break;
    }
    if (m_fill_kind == FillKind::Filled)
        m_fill_painter.begin(p);
}

void PathPainter::edge_to(Point<float> p)
{
    switch (m_stroke_kind) {
    case StrokeKind::OpenStroke:
    case StrokeKind::ClosedStroke:
        m_stroke_painter.stroke_to(p);
        break;
    case StrokeKind::NoStroke:
        break;
    }
    if (m_fill_kind == FillKind::Filled)
        m_fill_painter.edge_to(p);
}

void PathPainter::set_transform(AffineTransform const& transform)
{
    m_fill_painter.set_transform(transform);
    m_stroke_painter.set_transform(transform);
}

void PathPainter::set_clip_rect(const IntRect clip_rect)
{
    m_fill_painter.set_clip_rect(clip_rect);
    m_stroke_painter.set_clip_rect(clip_rect);
}

}
