/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FillPathImplementation.h"
#include <AK/Function.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Path.h>

static float fractional_part(float x)
{
    return x - floorf(x);
}

// Base algorithm from https://en.wikipedia.org/wiki/Xiaolin_Wu%27s_line_algorithm,
// because there seems to be no other known method for drawing AA'd lines (?)
template<Gfx::AntiAliasingPainter::AntiAliasPolicy policy>
void Gfx::AntiAliasingPainter::draw_anti_aliased_line(FloatPoint const& actual_from, FloatPoint const& actual_to, Color color, float thickness, Gfx::Painter::LineStyle style, Color)
{
    // FIXME: Implement this :P
    VERIFY(style == Painter::LineStyle::Solid);

    auto corrected_thickness = thickness > 1 ? thickness - 1 : thickness;
    auto size = IntSize(corrected_thickness, corrected_thickness);
    auto draw_point = [&](FloatPoint const& point, Color color) {
        auto center = m_transform.map(point).to_type<int>();
        m_underlying_painter.fill_rect(Gfx::IntRect::centered_on(center, size), color);
    };

    auto color_with_alpha = [&color](float new_alpha) {
        return color.with_alpha(color.alpha() * new_alpha);
    };

    auto actual_distance = actual_to - actual_from;
    auto from = actual_from;
    auto to = actual_to;
    auto is_steep = fabsf(actual_distance.y()) > fabsf(actual_distance.x());

    if (is_steep) {
        from = { from.y(), from.x() };
        to = { to.y(), to.x() };
    }

    if (from.x() > to.x())
        swap(from, to);

    auto distance = to - from;
    auto gradient = fabsf(distance.x()) < 1e-10f ? 1.0f : distance.y() / distance.x();

    auto draw_one_end = [&](auto& point) {
        auto end_x = roundf(point.x());
        auto end_point = FloatPoint { end_x, point.y() + gradient * (end_x - point.x()) };
        auto x_gap = 1 - fractional_part(point.x() + 0.5f);
        auto current_point = FloatPoint { end_point.x(), floorf(end_point.y()) };

        if (is_steep) {
            draw_point({ current_point.y(), current_point.x() }, color_with_alpha(x_gap * (1 - fractional_part(end_point.y()))));
            draw_point({ current_point.y() + 1, current_point.x() }, color_with_alpha(x_gap * fractional_part(end_point.y())));
        } else {
            draw_point(current_point, color_with_alpha(x_gap * (1 - fractional_part(end_point.y())) * 255));
            draw_point({ current_point.x(), current_point.y() + 1 }, color_with_alpha(x_gap * fractional_part(end_point.y())));
        }
        return end_point;
    };

    auto first_end_point = draw_one_end(from);
    auto last_end_point = draw_one_end(to);

    auto next_intersection = first_end_point.y() + gradient;
    auto delta_x = 0.7f; // Should be max(fabsf(sin_x), fabsf(cos_x)) with fewer samples needed if the line is axis-aligned.
                         // but there's no point in doing expensive calculations when the delta range is so small (0.7-1.0)
                         // so instead, just pick the smallest delta.
    auto delta_y = gradient * delta_x;

    auto x = first_end_point.x();
    while (x < last_end_point.x()) {
        if (is_steep) {
            if constexpr (policy == AntiAliasPolicy::OnlyEnds) {
                draw_point({ floorf(next_intersection), x }, color);
            } else {
                draw_point({ floorf(next_intersection), x }, color_with_alpha(1 - fractional_part(next_intersection)));
            }
            draw_point({ floorf(next_intersection) + 1, x }, color_with_alpha(fractional_part(next_intersection)));
        } else {
            if constexpr (policy == AntiAliasPolicy::OnlyEnds) {
                draw_point({ x, floorf(next_intersection) }, color);
            } else {
                draw_point({ x, floorf(next_intersection) }, color_with_alpha(1 - fractional_part(next_intersection)));
            }
            draw_point({ x, floorf(next_intersection) + 1 }, color_with_alpha(fractional_part(next_intersection)));
        }
        next_intersection += delta_y;
        x += delta_x;
    }
}

void Gfx::AntiAliasingPainter::draw_aliased_line(FloatPoint const& actual_from, FloatPoint const& actual_to, Color color, float thickness, Gfx::Painter::LineStyle style, Color alternate_color)
{
    draw_anti_aliased_line<AntiAliasPolicy::OnlyEnds>(actual_from, actual_to, color, thickness, style, alternate_color);
}

void Gfx::AntiAliasingPainter::draw_line(FloatPoint const& actual_from, FloatPoint const& actual_to, Color color, float thickness, Gfx::Painter::LineStyle style, Color alternate_color)
{
    draw_anti_aliased_line<AntiAliasPolicy::Full>(actual_from, actual_to, color, thickness, style, alternate_color);
}

void Gfx::AntiAliasingPainter::fill_path(Path& path, Color color, Painter::WindingRule rule)
{
    Detail::fill_path<Detail::FillPathMode::AllowFloatingPoints>(*this, path, color, rule);
}

void Gfx::AntiAliasingPainter::stroke_path(Path const& path, Color color, float thickness)
{
    FloatPoint cursor;

    for (auto& segment : path.segments()) {
        switch (segment.type()) {
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
        case Segment::Type::MoveTo:
            cursor = segment.point();
            break;
        case Segment::Type::LineTo:
            draw_line(cursor, segment.point(), color, thickness);
            cursor = segment.point();
            break;
        case Segment::Type::QuadraticBezierCurveTo: {
            auto& through = static_cast<QuadraticBezierCurveSegment const&>(segment).through();
            draw_quadratic_bezier_curve(through, cursor, segment.point(), color, thickness);
            cursor = segment.point();
            break;
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<CubicBezierCurveSegment const&>(segment);
            auto& through_0 = curve.through_0();
            auto& through_1 = curve.through_1();
            draw_cubic_bezier_curve(through_0, through_1, cursor, segment.point(), color, thickness);
            cursor = segment.point();
            break;
        }
        case Segment::Type::EllipticalArcTo:
            auto& arc = static_cast<EllipticalArcSegment const&>(segment);
            draw_elliptical_arc(cursor, segment.point(), arc.center(), arc.radii(), arc.x_axis_rotation(), arc.theta_1(), arc.theta_delta(), color, thickness);
            cursor = segment.point();
            break;
        }
    }
}

void Gfx::AntiAliasingPainter::draw_elliptical_arc(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& center, FloatPoint const& radii, float x_axis_rotation, float theta_1, float theta_delta, Color color, float thickness, Painter::LineStyle style)
{
    Gfx::Painter::for_each_line_segment_on_elliptical_arc(p1, p2, center, radii, x_axis_rotation, theta_1, theta_delta, [&](FloatPoint const& fp1, FloatPoint const& fp2) {
        draw_line(fp1, fp2, color, thickness, style);
    });
}

void Gfx::AntiAliasingPainter::draw_quadratic_bezier_curve(FloatPoint const& control_point, FloatPoint const& p1, FloatPoint const& p2, Color color, float thickness, Painter::LineStyle style)
{
    Gfx::Painter::for_each_line_segment_on_bezier_curve(control_point, p1, p2, [&](FloatPoint const& fp1, FloatPoint const& fp2) {
        draw_line(fp1, fp2, color, thickness, style);
    });
}

void Gfx::AntiAliasingPainter::draw_cubic_bezier_curve(const FloatPoint& control_point_0, const FloatPoint& control_point_1, const FloatPoint& p1, const FloatPoint& p2, Color color, float thickness, Painter::LineStyle style)
{
    Gfx::Painter::for_each_line_segment_on_cubic_bezier_curve(control_point_0, control_point_1, p1, p2, [&](FloatPoint const& fp1, FloatPoint const& fp2) {
        draw_line(fp1, fp2, color, thickness, style);
    });
}
