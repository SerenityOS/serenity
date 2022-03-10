/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, Ben Maxwell <macdue@dueutil.tech>
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

void Gfx::AntiAliasingPainter::draw_circle(IntPoint center, int radius, Color color)
{
    /*
    Algorithm from: https://cs.uwaterloo.ca/research/tr/1984/CS-84-38.pdf
    Inline comments are from the paper.
    */

    // TODO: Generalize to ellipses (see paper)

    // These happen to be the same here, but are treated separately in the paper:
    // intensity is the fill alpha
    const int intensity = color.alpha();
    // 0 to subpixel_resolution is the range of alpha values for the circle edges
    const int subpixel_resolution = intensity;

    // Note: Variable names below are based off the paper

    // Current pixel address
    int i = 0;
    int q = radius;

    // 1st and 2nd order differences of y
    int delta_y = 0;
    int delta2_y = 0;

    // Exact and predicted values of f(i) -- the circle equation scaled by subpixel_resolution
    int y = subpixel_resolution * radius;
    int y_hat = 0;

    // The value of f(i)*f(i)
    int f_squared = y * y;

    // 1st and 2nd order differences of f(i)*f(i)
    int delta_f_squared = subpixel_resolution * subpixel_resolution;
    int delta2_f_squared = -delta_f_squared - delta_f_squared;

    // edge_intersection_area/subpixel_resolution = percentage of pixel intersected by circle
    // (aka the alpha for the pixel)
    int edge_intersection_area = 0;
    int old_area = edge_intersection_area;

    auto predict = [&] {
        delta_y += delta2_y;
        // y_hat is the predicted value of f(i)
        y_hat = y + delta_y;
    };

    auto minimize = [&] {
        // Initialize the minimization
        delta_f_squared += delta2_f_squared;
        f_squared += delta_f_squared;

        int min_squared_error = y_hat * y_hat - f_squared;
        int prediction_overshot = 1;
        y = y_hat;

        // Force error negative
        if (min_squared_error > 0) {
            min_squared_error = -min_squared_error;
            prediction_overshot = -1;
        }

        // Minimize
        int previous_error = min_squared_error;
        while (min_squared_error < 0) {
            y += prediction_overshot;
            previous_error = min_squared_error;
            min_squared_error += y + y - prediction_overshot;
        }

        if (min_squared_error + previous_error > 0)
            y -= prediction_overshot;
    };

    auto correct = [&] {
        int error = y - y_hat;
        delta2_y += error;
        delta_y += error;
    };

    auto pixel = [&](int x, int y, int alpha) {
        if (alpha <= 0 || alpha > 255)
            return;
        auto pixel_colour = color;
        pixel_colour.set_alpha(alpha);
        m_underlying_painter.set_pixel(center + IntPoint { x, y }, pixel_colour, true);
    };

    auto fill = [&](int x, int ymax, int ymin, int alpha) {
        while (ymin <= ymax) {
            pixel(x, ymin, alpha);
            ymin += 1;
        }
    };

    auto eight_pixel = [&](int x, int y, int alpha) {
        pixel(x, y, alpha);
        pixel(x, -y - 1, alpha);
        pixel(-x - 1, -y - 1, alpha);
        pixel(-x - 1, y, alpha);
        pixel(y, x, alpha);
        pixel(y, -x - 1, alpha);
        pixel(-y - 1, -x - 1, alpha);
        pixel(-y - 1, x, alpha);
    };

    while (i < q) {
        predict();
        minimize();
        correct();
        old_area = edge_intersection_area;
        edge_intersection_area += delta_y;
        if (edge_intersection_area >= 0) {
            // Single pixel on perimeter
            eight_pixel(i, q, (edge_intersection_area + old_area) / 2);
            fill(i, q - 1, -q, intensity);
            fill(-i - 1, q - 1, -q, intensity);
        } else {
            // Two pixels on perimeter
            edge_intersection_area += subpixel_resolution;
            eight_pixel(i, q, old_area / 2);
            q -= 1;
            fill(i, q - 1, -q, intensity);
            fill(-i - 1, q - 1, -q, intensity);
            if (i < q) {
                // Haven't gone below the diagonal
                eight_pixel(i, q, (edge_intersection_area + subpixel_resolution) / 2);
                fill(q, i - 1, -i, intensity);
                fill(-q - 1, i - 1, -i, intensity);
            } else {
                // Went below the diagonal, fix edge_intersection_area for final pixels
                edge_intersection_area += subpixel_resolution;
            }
        }
        i += 1;
    }

    // Fill in 4 remaning pixels
    int alpha = edge_intersection_area / 2;
    pixel(q, q, alpha);
    pixel(-q - 1, q, alpha);
    pixel(-q - 1, -q - 1, alpha);
    pixel(q, -q - 1, alpha);
}

void Gfx::AntiAliasingPainter::fill_rect_with_rounded_corners(IntRect const& a_rect, Color color, int radius)
{
    fill_rect_with_rounded_corners(a_rect, color, radius, radius, radius, radius);
}

void Gfx::AntiAliasingPainter::fill_rect_with_rounded_corners(IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius)
{
    if (!top_left_radius && !top_right_radius && !bottom_right_radius && !bottom_left_radius)
        return m_underlying_painter.fill_rect(a_rect, color);

    if (color.alpha() == 0)
        return;

    IntPoint top_left_corner {
        a_rect.x() + top_left_radius,
        a_rect.y() + top_left_radius,
    };
    IntPoint top_right_corner {
        a_rect.x() + a_rect.width() - top_right_radius,
        a_rect.y() + top_right_radius,
    };
    IntPoint bottom_right_corner {
        a_rect.x() + bottom_left_radius,
        a_rect.y() + a_rect.height() - bottom_right_radius
    };
    IntPoint bottom_left_corner {
        a_rect.x() + a_rect.width() - bottom_left_radius,
        a_rect.y() + a_rect.height() - bottom_left_radius
    };

    IntRect top_rect {
        a_rect.x() + top_left_radius,
        a_rect.y(),
        a_rect.width() - top_left_radius - top_right_radius,
        top_left_radius
    };
    IntRect right_rect {
        a_rect.x() + a_rect.width() - top_right_radius,
        a_rect.y() + top_right_radius,
        top_right_radius,
        a_rect.height() - top_right_radius - bottom_right_radius
    };
    IntRect bottom_rect {
        a_rect.x() + bottom_left_radius,
        a_rect.y() + a_rect.height() - bottom_right_radius,
        a_rect.width() - bottom_left_radius - bottom_right_radius,
        bottom_right_radius
    };
    IntRect left_rect {
        a_rect.x(),
        a_rect.y() + top_left_radius,
        bottom_left_radius,
        a_rect.height() - top_left_radius - bottom_left_radius
    };

    IntRect inner = {
        left_rect.x() + left_rect.width(),
        left_rect.y(),
        a_rect.width() - left_rect.width() - right_rect.width(),
        a_rect.height() - top_rect.height() - bottom_rect.height()
    };

    m_underlying_painter.fill_rect(top_rect, color);
    m_underlying_painter.fill_rect(right_rect, color);
    m_underlying_painter.fill_rect(bottom_rect, color);
    m_underlying_painter.fill_rect(left_rect, color);
    m_underlying_painter.fill_rect(inner, color);

    // FIXME: Don't draw a whole circle each time
    if (top_left_radius)
        draw_circle(top_left_corner, top_left_radius, color);
    if (top_right_radius)
        draw_circle(top_right_corner, top_right_radius, color);
    if (bottom_left_radius)
        draw_circle(bottom_left_corner, bottom_left_radius, color);
    if (bottom_right_radius)
        draw_circle(bottom_right_corner, bottom_right_radius, color);
}
