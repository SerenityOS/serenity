/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/Math.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>

namespace Gfx {

void Path::elliptical_arc_to(FloatPoint point, FloatSize radii, float x_axis_rotation, bool large_arc, bool sweep)
{
    auto next_point = point;

    double rx = radii.width();
    double ry = radii.height();

    double x_axis_rotation_s;
    double x_axis_rotation_c;
    AK::sincos(static_cast<double>(x_axis_rotation), x_axis_rotation_s, x_axis_rotation_c);

    // Find the last point
    FloatPoint last_point { 0, 0 };
    if (!m_segments.is_empty())
        last_point = m_segments.last()->point();

    // Step 1 of out-of-range radii correction
    if (rx == 0.0 || ry == 0.0) {
        append_segment<LineSegment>(next_point);
        return;
    }

    // Step 2 of out-of-range radii correction
    if (rx < 0)
        rx *= -1.0;
    if (ry < 0)
        ry *= -1.0;

    // POSSIBLY HACK: Handle the case where both points are the same.
    auto same_endpoints = next_point == last_point;
    if (same_endpoints) {
        if (!large_arc) {
            // Nothing is going to be drawn anyway.
            return;
        }

        // Move the endpoint by a small amount to avoid division by zero.
        next_point.translate_by(0.01f, 0.01f);
    }

    // Find (cx, cy), theta_1, theta_delta
    // Step 1: Compute (x1', y1')
    auto x_avg = static_cast<double>(last_point.x() - next_point.x()) / 2.0;
    auto y_avg = static_cast<double>(last_point.y() - next_point.y()) / 2.0;
    auto x1p = x_axis_rotation_c * x_avg + x_axis_rotation_s * y_avg;
    auto y1p = -x_axis_rotation_s * x_avg + x_axis_rotation_c * y_avg;

    // Step 2: Compute (cx', cy')
    double x1p_sq = x1p * x1p;
    double y1p_sq = y1p * y1p;
    double rx_sq = rx * rx;
    double ry_sq = ry * ry;

    // Step 3 of out-of-range radii correction
    double lambda = x1p_sq / rx_sq + y1p_sq / ry_sq;
    double multiplier;

    if (lambda > 1.0) {
        auto lambda_sqrt = AK::sqrt(lambda);
        rx *= lambda_sqrt;
        ry *= lambda_sqrt;
        multiplier = 0.0;
    } else {
        double numerator = rx_sq * ry_sq - rx_sq * y1p_sq - ry_sq * x1p_sq;
        double denominator = rx_sq * y1p_sq + ry_sq * x1p_sq;
        multiplier = AK::sqrt(numerator / denominator);
    }

    if (large_arc == sweep)
        multiplier *= -1.0;

    double cxp = multiplier * rx * y1p / ry;
    double cyp = multiplier * -ry * x1p / rx;

    // Step 3: Compute (cx, cy) from (cx', cy')
    x_avg = (last_point.x() + next_point.x()) / 2.0f;
    y_avg = (last_point.y() + next_point.y()) / 2.0f;
    double cx = x_axis_rotation_c * cxp - x_axis_rotation_s * cyp + x_avg;
    double cy = x_axis_rotation_s * cxp + x_axis_rotation_c * cyp + y_avg;

    double theta_1 = AK::atan2((y1p - cyp) / ry, (x1p - cxp) / rx);
    double theta_2 = AK::atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);

    auto theta_delta = theta_2 - theta_1;

    if (!sweep && theta_delta > 0.0) {
        theta_delta -= 2 * AK::Pi<double>;
    } else if (sweep && theta_delta < 0) {
        theta_delta += 2 * AK::Pi<double>;
    }

    elliptical_arc_to(
        next_point,
        { cx, cy },
        { rx, ry },
        x_axis_rotation,
        theta_1,
        theta_delta,
        large_arc,
        sweep);
}

void Path::close()
{
    if (m_segments.size() <= 1)
        return;

    auto last_point = m_segments.last()->point();

    for (ssize_t i = m_segments.size() - 1; i >= 0; --i) {
        auto& segment = m_segments[i];
        if (segment->type() == Segment::Type::MoveTo) {
            if (last_point == segment->point())
                return;
            append_segment<LineSegment>(segment->point());
            invalidate_split_lines();
            return;
        }
    }
}

void Path::close_all_subpaths()
{
    if (m_segments.size() <= 1)
        return;

    invalidate_split_lines();

    Optional<FloatPoint> cursor, start_of_subpath;
    bool is_first_point_in_subpath { false };

    auto close_previous_subpath = [&] {
        if (cursor.has_value() && !is_first_point_in_subpath) {
            // This is a move from a subpath to another
            // connect the two ends of this subpath before
            // moving on to the next one
            VERIFY(start_of_subpath.has_value());

            append_segment<MoveSegment>(cursor.value());
            append_segment<LineSegment>(start_of_subpath.value());
        }
    };

    auto segment_count = m_segments.size();
    for (size_t i = 0; i < segment_count; i++) {
        // Note: We need to use m_segments[i] as append_segment() may invalidate any references.
        switch (m_segments[i]->type()) {
        case Segment::Type::MoveTo: {
            close_previous_subpath();
            is_first_point_in_subpath = true;
            cursor = m_segments[i]->point();
            break;
        }
        case Segment::Type::LineTo:
        case Segment::Type::QuadraticBezierCurveTo:
        case Segment::Type::CubicBezierCurveTo:
        case Segment::Type::EllipticalArcTo:
            if (is_first_point_in_subpath) {
                start_of_subpath = cursor;
                is_first_point_in_subpath = false;
            }
            cursor = m_segments[i]->point();
            break;
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
            break;
        }
    }

    if (m_segments.last()->type() != Segment::Type::MoveTo)
        close_previous_subpath();
}

DeprecatedString Path::to_deprecated_string() const
{
    StringBuilder builder;
    builder.append("Path { "sv);
    for (auto& segment : m_segments) {
        switch (segment->type()) {
        case Segment::Type::MoveTo:
            builder.append("MoveTo"sv);
            break;
        case Segment::Type::LineTo:
            builder.append("LineTo"sv);
            break;
        case Segment::Type::QuadraticBezierCurveTo:
            builder.append("QuadraticBezierCurveTo"sv);
            break;
        case Segment::Type::CubicBezierCurveTo:
            builder.append("CubicBezierCurveTo"sv);
            break;
        case Segment::Type::EllipticalArcTo:
            builder.append("EllipticalArcTo"sv);
            break;
        case Segment::Type::Invalid:
            builder.append("Invalid"sv);
            break;
        }
        builder.appendff("({}", segment->point());

        switch (segment->type()) {
        case Segment::Type::QuadraticBezierCurveTo:
            builder.append(", "sv);
            builder.append(static_cast<QuadraticBezierCurveSegment const&>(*segment).through().to_deprecated_string());
            break;
        case Segment::Type::CubicBezierCurveTo:
            builder.append(", "sv);
            builder.append(static_cast<CubicBezierCurveSegment const&>(*segment).through_0().to_deprecated_string());
            builder.append(", "sv);
            builder.append(static_cast<CubicBezierCurveSegment const&>(*segment).through_1().to_deprecated_string());
            break;
        case Segment::Type::EllipticalArcTo: {
            auto& arc = static_cast<EllipticalArcSegment const&>(*segment);
            builder.appendff(", {}, {}, {}, {}, {}",
                arc.radii().to_deprecated_string().characters(),
                arc.center().to_deprecated_string().characters(),
                arc.x_axis_rotation(),
                arc.theta_1(),
                arc.theta_delta());
            break;
        }
        default:
            break;
        }

        builder.append(") "sv);
    }
    builder.append('}');
    return builder.to_deprecated_string();
}

void Path::segmentize_path()
{
    Vector<FloatLine> segments;
    float min_x = 0;
    float min_y = 0;
    float max_x = 0;
    float max_y = 0;

    bool first = true;
    auto add_point_to_bbox = [&](Gfx::FloatPoint point) {
        float x = point.x();
        float y = point.y();
        if (first) {
            min_x = max_x = x;
            min_y = max_y = y;
            first = false;
        } else {
            min_x = min(min_x, x);
            min_y = min(min_y, y);
            max_x = max(max_x, x);
            max_y = max(max_y, y);
        }
    };

    auto add_line = [&](auto const& p0, auto const& p1) {
        segments.append({ p0, p1 });
        add_point_to_bbox(p1);
    };

    FloatPoint cursor { 0, 0 };
    for (auto& segment : m_segments) {
        switch (segment->type()) {
        case Segment::Type::MoveTo:
            add_point_to_bbox(segment->point());
            cursor = segment->point();
            break;
        case Segment::Type::LineTo: {
            add_line(cursor, segment->point());
            cursor = segment->point();
            break;
        }
        case Segment::Type::QuadraticBezierCurveTo: {
            auto control = static_cast<QuadraticBezierCurveSegment const&>(*segment).through();
            Painter::for_each_line_segment_on_bezier_curve(control, cursor, segment->point(), [&](FloatPoint p0, FloatPoint p1) {
                add_line(p0, p1);
            });
            cursor = segment->point();
            break;
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<CubicBezierCurveSegment const&>(*segment);
            auto control_0 = curve.through_0();
            auto control_1 = curve.through_1();
            Painter::for_each_line_segment_on_cubic_bezier_curve(control_0, control_1, cursor, segment->point(), [&](FloatPoint p0, FloatPoint p1) {
                add_line(p0, p1);
            });
            cursor = segment->point();
            break;
        }
        case Segment::Type::EllipticalArcTo: {
            auto& arc = static_cast<EllipticalArcSegment const&>(*segment);
            Painter::for_each_line_segment_on_elliptical_arc(cursor, arc.point(), arc.center(), arc.radii(), arc.x_axis_rotation(), arc.theta_1(), arc.theta_delta(), [&](FloatPoint p0, FloatPoint p1) {
                add_line(p0, p1);
            });
            cursor = segment->point();
            break;
        }
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
        }

        first = false;
    }

    m_split_lines = move(segments);
    m_bounding_box = Gfx::FloatRect { min_x, min_y, max_x - min_x, max_y - min_y };
}

Path Path::copy_transformed(Gfx::AffineTransform const& transform) const
{
    Path result;

    for (auto const& segment : m_segments) {
        switch (segment->type()) {
        case Segment::Type::MoveTo:
            result.move_to(transform.map(segment->point()));
            break;
        case Segment::Type::LineTo: {
            result.line_to(transform.map(segment->point()));
            break;
        }
        case Segment::Type::QuadraticBezierCurveTo: {
            auto const& quadratic_segment = static_cast<QuadraticBezierCurveSegment const&>(*segment);
            result.quadratic_bezier_curve_to(transform.map(quadratic_segment.through()), transform.map(segment->point()));
            break;
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto const& cubic_segment = static_cast<CubicBezierCurveSegment const&>(*segment);
            result.cubic_bezier_curve_to(transform.map(cubic_segment.through_0()), transform.map(cubic_segment.through_1()), transform.map(segment->point()));
            break;
        }
        case Segment::Type::EllipticalArcTo: {
            auto const& arc_segment = static_cast<EllipticalArcSegment const&>(*segment);
            auto det_negative = transform.determinant() < 0;
            result.elliptical_arc_to(
                transform.map(segment->point()),
                transform.map(arc_segment.center()),
                transform.map(arc_segment.radii()),
                arc_segment.x_axis_rotation() + transform.rotation(),
                det_negative ? AK::Pi<float> * 2 - arc_segment.theta_1() : arc_segment.theta_1(),
                det_negative ? -arc_segment.theta_delta() : arc_segment.theta_delta(),
                arc_segment.large_arc(),
                det_negative ? !arc_segment.sweep() : arc_segment.sweep());
            break;
        }
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
        }
    }

    return result;
}

void Path::add_path(Path const& other)
{
    m_segments.extend(other.m_segments);
    invalidate_split_lines();
}

template<typename T>
struct RoundTrip {
    RoundTrip(ReadonlySpan<T> span)
        : m_span(span)
    {
    }

    size_t size() const
    {
        return m_span.size() * 2 - 1;
    }

    T const& operator[](size_t index) const
    {
        // Follow the path:
        if (index < m_span.size())
            return m_span[index];
        // Then in reverse:
        if (index < size())
            return m_span[size() - index - 1];
        // Then wrap around again:
        return m_span[index - size() + 1];
    }

private:
    ReadonlySpan<T> m_span;
};

Path Path::stroke_to_fill(float thickness) const
{
    // Note: This convolves a polygon with the path using the algorithm described
    // in https://keithp.com/~keithp/talks/cairo2003.pdf (3.1 Stroking Splines via Convolution)

    auto& lines = split_lines();
    if (lines.is_empty())
        return Path {};

    // Paths can be disconnected, which a pain to deal with, so split it up.
    Vector<Vector<FloatPoint>> segments;
    segments.append({ lines.first().a() });
    for (auto& line : lines) {
        if (line.a() == segments.last().last()) {
            segments.last().append(line.b());
        } else {
            segments.append({ line.a(), line.b() });
        }
    }

    // Note: This is the same as the tolerance from bezier curve splitting.
    constexpr auto flatness = 0.015f;
    auto pen_vertex_count = max(
        static_cast<int>(ceilf(AK::Pi<float> / acosf(1 - (2 * flatness) / thickness))), 4);
    if (pen_vertex_count % 2 == 1)
        pen_vertex_count += 1;

    Vector<FloatPoint, 128> pen_vertices;
    pen_vertices.ensure_capacity(pen_vertex_count);

    // Generate vertices for the pen (going counterclockwise). The pen does not necessarily need
    // to be a circle (or an approximation of one), but other shapes are untested.
    float theta = 0;
    float theta_delta = (AK::Pi<float> * 2) / pen_vertex_count;
    for (int i = 0; i < pen_vertex_count; i++) {
        float sin_theta;
        float cos_theta;
        AK::sincos(theta, sin_theta, cos_theta);
        pen_vertices.unchecked_append({ cos_theta * thickness / 2, sin_theta * thickness / 2 });
        theta -= theta_delta;
    }

    auto wrapping_index = [](auto& vertices, auto index) {
        return vertices[(index + vertices.size()) % vertices.size()];
    };

    auto angle_between = [](auto p1, auto p2) {
        auto delta = p2 - p1;
        return atan2f(delta.y(), delta.x());
    };

    struct ActiveRange {
        float start;
        float end;

        bool in_range(float angle) const
        {
            // Note: Since active ranges go counterclockwise start > end unless we wrap around at 180 degrees
            return ((angle <= start && angle >= end)
                || (start < end && angle <= start)
                || (start < end && angle >= end));
        }
    };

    Vector<ActiveRange, 128> active_ranges;
    active_ranges.ensure_capacity(pen_vertices.size());
    for (auto i = 0; i < pen_vertex_count; i++) {
        active_ranges.unchecked_append({ angle_between(wrapping_index(pen_vertices, i - 1), pen_vertices[i]),
            angle_between(pen_vertices[i], wrapping_index(pen_vertices, i + 1)) });
    }

    auto clockwise = [](float current_angle, float target_angle) {
        if (target_angle < 0)
            target_angle += AK::Pi<float> * 2;
        if (current_angle < 0)
            current_angle += AK::Pi<float> * 2;
        if (target_angle < current_angle)
            target_angle += AK::Pi<float> * 2;
        return (target_angle - current_angle) <= AK::Pi<float>;
    };

    Path convolution;
    for (auto& segment : segments) {
        RoundTrip<FloatPoint> shape { segment };

        bool first = true;
        auto add_vertex = [&](auto v) {
            if (first) {
                convolution.move_to(v);
                first = false;
            } else {
                convolution.line_to(v);
            }
        };

        auto shape_idx = 0u;

        auto slope = [&] {
            return angle_between(shape[shape_idx], shape[shape_idx + 1]);
        };

        auto start_slope = slope();
        // Note: At least one range must be active.
        auto active = *active_ranges.find_first_index_if([&](auto& range) {
            return range.in_range(start_slope);
        });

        while (shape_idx < shape.size()) {
            add_vertex(shape[shape_idx] + pen_vertices[active]);
            auto slope_now = slope();
            auto range = active_ranges[active];
            if (range.in_range(slope_now)) {
                shape_idx++;
            } else {
                if (clockwise(slope_now, range.end)) {
                    if (active == static_cast<size_t>(pen_vertex_count - 1))
                        active = 0;
                    else
                        active++;
                } else {
                    if (active == 0)
                        active = pen_vertex_count - 1;
                    else
                        active--;
                }
            }
        }
    }

    return convolution;
}

}
