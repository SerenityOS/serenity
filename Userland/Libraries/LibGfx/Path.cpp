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

void Path::approximate_elliptical_arc_with_cubic_beziers(FloatPoint center, FloatSize radii, float x_axis_rotation, float theta, float theta_delta)
{
    float sin_x_rotation;
    float cos_x_rotation;
    AK::sincos(x_axis_rotation, sin_x_rotation, cos_x_rotation);
    auto arc_point_and_derivative = [&](float t, FloatPoint& point, FloatPoint& derivative) {
        float sin_angle;
        float cos_angle;
        AK::sincos(t, sin_angle, cos_angle);
        point = FloatPoint {
            center.x()
                + radii.width() * cos_x_rotation * cos_angle
                - radii.height() * sin_x_rotation * sin_angle,
            center.y()
                + radii.width() * sin_x_rotation * cos_angle
                + radii.height() * cos_x_rotation * sin_angle,
        };
        derivative = FloatPoint {
            -radii.width() * cos_x_rotation * sin_angle
                - radii.height() * sin_x_rotation * cos_angle,
            -radii.width() * sin_x_rotation * sin_angle
                + radii.height() * cos_x_rotation * cos_angle,
        };
    };
    auto approximate_arc_between = [&](float start_angle, float end_angle) {
        auto t = AK::tan((end_angle - start_angle) / 2);
        auto alpha = AK::sin(end_angle - start_angle) * ((AK::sqrt(4 + 3 * t * t) - 1) / 3);
        FloatPoint p1, d1;
        FloatPoint p2, d2;
        arc_point_and_derivative(start_angle, p1, d1);
        arc_point_and_derivative(end_angle, p2, d2);
        auto q1 = p1 + d1.scaled(alpha, alpha);
        auto q2 = p2 - d2.scaled(alpha, alpha);
        cubic_bezier_curve_to(q1, q2, p2);
    };
    // FIXME: Come up with a more mathematically sound step size (using some error calculation).
    auto step = theta_delta;
    int step_count = 1;
    while (fabs(step) > AK::Pi<float> / 4) {
        step /= 2;
        step_count *= 2;
    }
    float prev = theta;
    float t = prev + step;
    for (int i = 0; i < step_count; i++, prev = t, t += step)
        approximate_arc_between(prev, t);
}

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

    approximate_elliptical_arc_with_cubic_beziers(
        { cx, cy },
        { rx, ry },
        x_axis_rotation,
        theta_1,
        theta_delta);
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

Path Path::stroke_to_fill(float thickness, StrokeLinecap linecap, StrokeLinejoin linejoin) const
{
    VERIFY(thickness > 0);

    Path stroked_path;
    Path outer_offset_path;
    Path inner_offset_path;

    auto parallel_line = [](FloatPoint const& prev, FloatPoint const& curr, float thickness, bool outer) {
        auto sign = outer ? 1 : -1;
        auto x1 = prev.x();
        auto y1 = prev.y();
        auto x2 = curr.x();
        auto y2 = curr.y();
        auto sum = (y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1);
        auto y_sign = (x2 > x1) ? 1 : -1;
        auto x_sign = (y2 > y1) ? 1 : -1;

        auto m1 = x1 + sign * x_sign * (thickness / 2) * AK::sqrt((y2 - y1) * (y2 - y1) / sum);
        auto n1 = y1 - sign * y_sign * (thickness / 2) * AK::sqrt((x2 - x1) * (x2 - x1) / sum);
        auto m2 = x2 + sign * x_sign * (thickness / 2) * AK::sqrt((y2 - y1) * (y2 - y1) / sum);
        auto n2 = y2 - sign * y_sign * (thickness / 2) * AK::sqrt((x2 - x1) * (x2 - x1) / sum);

        return FloatLine { FloatPoint { m1, n1 }, FloatPoint { m2, n2 } };
    };

    auto compute_derivative = [](Segment const* segment, FloatPoint cursor, bool end = true) {
        switch (segment->type()) {
        case Segment::Type::MoveTo:
        case Segment::Type::LineTo:
            return cursor - segment->point();
        case Segment::Type::QuadraticBezierCurveTo: {
            auto& curve = static_cast<QuadraticBezierCurveSegment const&>(*segment);
            if (end) {
                return curve.through() * (-2.0f) + curve.point() * 2.0f;
            }
            return cursor * (-2.0f) + curve.through();
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<CubicBezierCurveSegment const&>(*segment);
            if (end) {
                return curve.through_1() * (-3.0f) + curve.point() * 3.0f;
            }
            return cursor * (-3.0f) + curve.through_0() * 3.0f;
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    // This function is borrowed from Line::intersected method.
    // But here we also compute the joined point outside current lines.
    auto intersected_point = [&](FloatPoint const& outer_offset_point1, FloatPoint const& outer_offset_point2, Segment const* prev_segment, Segment const* curr_segment, FloatPoint const& prev_point) -> Optional<FloatPoint> {
        auto cross_product = [](FloatPoint const& p1, FloatPoint const& p2) {
            return p1.x() * p2.y() - p1.y() * p2.x();
        };

        // For cubic/quad approximated curves, the approximated line at end points always can not reflect the real tangent direction.
        // So instead of using checking two aproximated line derivative, use the real derivative of the arc.
        auto r = compute_derivative(prev_segment, prev_point, true);
        auto s = compute_derivative(curr_segment, prev_segment->point(), false);

        auto delta_a = outer_offset_point2 - outer_offset_point1;
        auto denom = cross_product(r, s);

        // FIXME: 0.01 is an experimental number. If denom is too small, then t will be too large.
        if (AK::abs(denom) < 0.01f) {
            return {};
        }

        auto t = cross_product(delta_a, s) / denom;
        return FloatPoint { outer_offset_point1.x() + t * r.x(), outer_offset_point1.y() + t * r.y() };
    };

    auto join_lines = [&](FloatLine const& outer_line, FloatPoint const& joined_point, FloatPoint const& prev_point, Segment const* prev, Segment const* curr) {
        switch (linejoin) {
        case StrokeLinejoin::Arcs:
        case StrokeLinejoin::MiterClip:
            // FIXME: To implement arcs, miterclip
            outer_offset_path.line_to(outer_line.a());
            break;
        case StrokeLinejoin::Bevel:
            outer_offset_path.line_to(outer_line.a());
            break;
        case StrokeLinejoin::Miter: {
            VERIFY(outer_offset_path.segments().size() >= 2);
            auto intersected = intersected_point(outer_offset_path.segments().last()->point(), outer_line.a(), prev, curr, prev_point);
            if (intersected.has_value()) {
                outer_offset_path.line_to(intersected.value());
            }

            outer_offset_path.line_to(outer_line.a());
            break;
        }
        case StrokeLinejoin::Round:
            outer_offset_path.arc_to(
                outer_line.a(),
                outer_offset_path.segments().last()->point().distance_from(joined_point),
                false,
                true);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto append_offset_path = [&](FloatPoint const& start_before, FloatPoint const& start, FloatPoint const& end, Segment const* prev, Segment const* curr, bool joinable = true) {
        FloatLine outer_line = parallel_line(start, end, thickness, true);
        FloatLine inner_line = parallel_line(start, end, thickness, false);

        if (outer_offset_path.segments().size() == 0) {
            outer_offset_path.move_to(outer_line.a());
        } else if (joinable) {
            join_lines(outer_line, start, start_before, prev, curr);
        } else {
            outer_offset_path.line_to(outer_line.a());
        }
        outer_offset_path.line_to(outer_line.b());

        if (inner_offset_path.segments().size() == 0) {
            inner_offset_path.move_to(inner_line.a());
        }
        {
            inner_offset_path.line_to(inner_line.a());
        }
        inner_offset_path.line_to(inner_line.b());
    };

    auto add_stroke_line_cap = [&](FloatPoint p0) {
        switch (linecap) {
        case StrokeLinecap::Butt: {
            outer_offset_path.line_to(p0);
            break;
        }
        case StrokeLinecap::Square: {
            auto line = parallel_line(outer_offset_path.segments().last()->point(), p0, thickness, true);
            outer_offset_path.line_to(line.a());
            outer_offset_path.line_to(line.b());
            outer_offset_path.line_to(p0);
            break;
        }
        case StrokeLinecap::Round: {
            outer_offset_path.arc_to(p0, thickness / 2, true, true);
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto close_interval_stroke_path = [&](int head, int tail) {
        if (outer_offset_path.segments().size() == 0 || inner_offset_path.segments().size() == 0)
            return;

        // Case 1: It is a full closed geometry. Then it would be two seperate closed offset path: outer one and inner one.
        if (m_segments.at(head)->point() == m_segments.at(tail)->point()) {
            join_lines(
                FloatLine { outer_offset_path.segments().at(0)->point(), outer_offset_path.segments().at(1)->point() },
                m_segments.at(head)->point(),
                m_segments.at(tail - 1)->point(),
                m_segments.at(tail),
                m_segments.at(head + 1));
            // According to the NonZero filling rule, should inverse inner offset path to cancel out the outer offset path.
            // Then make the middle part to be empty.
            Path inversed_inner_offset_path;
            inversed_inner_offset_path.move_to(inner_offset_path.segments().last()->point());
            for (int i = inner_offset_path.segments().size() - 2; i >= 0; --i) {
                inversed_inner_offset_path.line_to(inner_offset_path.segments()[i]->point());
            }
            inversed_inner_offset_path.close();
            stroked_path.append_path(inversed_inner_offset_path);
        } else { // Case 2: Outer offset path linked with inner offset path.
            add_stroke_line_cap(inner_offset_path.segments().last()->point());

            for (int i = inner_offset_path.segments().size() - 2; i >= 0; --i) {
                outer_offset_path.line_to(inner_offset_path.segments()[i]->point());
            }

            add_stroke_line_cap(outer_offset_path.segments().first()->point());
        }

        stroked_path.append_path(outer_offset_path);
        outer_offset_path.clear();
        inner_offset_path.clear();
    };

    FloatPoint cursor;
    FloatPoint pre_cursor;
    int interval_head = 0;
    for (size_t i = 0; i < m_segments.size(); ++i) {
        auto segment = m_segments.at(i);
        switch (segment->type()) {
        case Segment::Type::MoveTo: {
            if (outer_offset_path.segments().size() > 0 && inner_offset_path.segments().size() > 0) {
                close_interval_stroke_path(interval_head, i - 1);
                interval_head = i;
            }
            break;
        }
        case Segment::Type::LineTo: {
            append_offset_path(pre_cursor, cursor, segment->point(), m_segments.at(i - 1), segment);
            break;
        }
        case Segment::Type::QuadraticBezierCurveTo: {
            auto control = static_cast<QuadraticBezierCurveSegment const&>(*segment).through();
            bool first = true;
            Painter::for_each_line_segment_on_bezier_curve(control, cursor, segment->point(), [&](FloatPoint p0, FloatPoint p1) {
                append_offset_path(pre_cursor, p0, p1, m_segments.at(i - 1), segment, first);
                first = false;
            });
            break;
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<CubicBezierCurveSegment const&>(*segment);
            auto control_0 = curve.through_0();
            auto control_1 = curve.through_1();
            bool first = true;
            Painter::for_each_line_segment_on_cubic_bezier_curve(control_0, control_1, cursor, segment->point(), [&](FloatPoint p0, FloatPoint p1) {
                append_offset_path(pre_cursor, p0, p1, m_segments.at(i - 1), segment, first);
                first = false;
            });
            break;
        }
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
        }
        pre_cursor = cursor;
        cursor = segment->point();
    }

    close_interval_stroke_path(interval_head, m_segments.size() - 1);
    return stroked_path;
}

}
