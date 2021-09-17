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

void Path::elliptical_arc_to(const FloatPoint& point, const FloatPoint& radii, double x_axis_rotation, bool large_arc, bool sweep)
{
    auto next_point = point;

    double rx = radii.x();
    double ry = radii.y();

    double x_axis_rotation_c = AK::cos(x_axis_rotation);
    double x_axis_rotation_s = AK::sin(x_axis_rotation);

    // Find the last point
    FloatPoint last_point { 0, 0 };
    if (!m_segments.is_empty())
        last_point = m_segments.last().point();

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
        theta_delta -= 2 * M_PI;
    } else if (sweep && theta_delta < 0) {
        theta_delta += 2 * M_PI;
    }

    elliptical_arc_to(
        next_point,
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

    auto& last_point = m_segments.last().point();

    for (ssize_t i = m_segments.size() - 1; i >= 0; --i) {
        auto& segment = m_segments[i];
        if (segment.type() == Segment::Type::MoveTo) {
            if (last_point == segment.point())
                return;
            append_segment<LineSegment>(segment.point());
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

    for (auto& segment : m_segments) {
        switch (segment.type()) {
        case Segment::Type::MoveTo: {
            if (cursor.has_value() && !is_first_point_in_subpath) {
                // This is a move from a subpath to another
                // connect the two ends of this subpath before
                // moving on to the next one
                VERIFY(start_of_subpath.has_value());

                append_segment<MoveSegment>(cursor.value());
                append_segment<LineSegment>(start_of_subpath.value());
            }
            is_first_point_in_subpath = true;
            cursor = segment.point();
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
            cursor = segment.point();
            break;
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
            break;
        }
    }
}

String Path::to_string() const
{
    StringBuilder builder;
    builder.append("Path { ");
    for (auto& segment : m_segments) {
        switch (segment.type()) {
        case Segment::Type::MoveTo:
            builder.append("MoveTo");
            break;
        case Segment::Type::LineTo:
            builder.append("LineTo");
            break;
        case Segment::Type::QuadraticBezierCurveTo:
            builder.append("QuadraticBezierCurveTo");
            break;
        case Segment::Type::CubicBezierCurveTo:
            builder.append("CubicBezierCurveTo");
            break;
        case Segment::Type::EllipticalArcTo:
            builder.append("EllipticalArcTo");
            break;
        case Segment::Type::Invalid:
            builder.append("Invalid");
            break;
        }
        builder.appendff("({}", segment.point());

        switch (segment.type()) {
        case Segment::Type::QuadraticBezierCurveTo:
            builder.append(", ");
            builder.append(static_cast<const QuadraticBezierCurveSegment&>(segment).through().to_string());
            break;
        case Segment::Type::CubicBezierCurveTo:
            builder.append(", ");
            builder.append(static_cast<const CubicBezierCurveSegment&>(segment).through_0().to_string());
            builder.append(", ");
            builder.append(static_cast<const CubicBezierCurveSegment&>(segment).through_1().to_string());
            break;
        case Segment::Type::EllipticalArcTo: {
            auto& arc = static_cast<const EllipticalArcSegment&>(segment);
            builder.appendff(", {}, {}, {}, {}, {}",
                arc.radii().to_string().characters(),
                arc.center().to_string().characters(),
                arc.x_axis_rotation(),
                arc.theta_1(),
                arc.theta_delta());
            break;
        }
        default:
            break;
        }

        builder.append(") ");
    }
    builder.append("}");
    return builder.to_string();
}

void Path::segmentize_path()
{
    Vector<SplitLineSegment> segments;
    float min_x = 0;
    float min_y = 0;
    float max_x = 0;
    float max_y = 0;

    auto add_point_to_bbox = [&](const Gfx::FloatPoint& point) {
        float x = point.x();
        float y = point.y();
        min_x = min(min_x, x);
        min_y = min(min_y, y);
        max_x = max(max_x, x);
        max_y = max(max_y, y);
    };

    auto add_line = [&](const auto& p0, const auto& p1) {
        float ymax = p0.y(), ymin = p1.y(), x_of_ymin = p1.x(), x_of_ymax = p0.x();
        auto slope = p0.x() == p1.x() ? 0 : ((float)(p0.y() - p1.y())) / ((float)(p0.x() - p1.x()));
        if (p0.y() < p1.y()) {
            swap(ymin, ymax);
            swap(x_of_ymin, x_of_ymax);
        }

        segments.append({ FloatPoint(p0.x(), p0.y()),
            FloatPoint(p1.x(), p1.y()),
            slope == 0 ? 0 : 1 / slope,
            x_of_ymin,
            ymax, ymin, x_of_ymax });

        add_point_to_bbox(p1);
    };

    FloatPoint cursor { 0, 0 };
    bool first = true;

    for (auto& segment : m_segments) {
        switch (segment.type()) {
        case Segment::Type::MoveTo:
            if (first) {
                min_x = segment.point().x();
                min_y = segment.point().y();
                max_x = segment.point().x();
                max_y = segment.point().y();
            } else {
                add_point_to_bbox(segment.point());
            }
            cursor = segment.point();
            break;
        case Segment::Type::LineTo: {
            add_line(cursor, segment.point());
            cursor = segment.point();
            break;
        }
        case Segment::Type::QuadraticBezierCurveTo: {
            auto& control = static_cast<QuadraticBezierCurveSegment&>(segment).through();
            Painter::for_each_line_segment_on_bezier_curve(control, cursor, segment.point(), [&](const FloatPoint& p0, const FloatPoint& p1) {
                add_line(p0, p1);
            });
            cursor = segment.point();
            break;
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<CubicBezierCurveSegment const&>(segment);
            auto& control_0 = curve.through_0();
            auto& control_1 = curve.through_1();
            Painter::for_each_line_segment_on_cubic_bezier_curve(control_0, control_1, cursor, segment.point(), [&](const FloatPoint& p0, const FloatPoint& p1) {
                add_line(p0, p1);
            });
            cursor = segment.point();
            break;
        }
        case Segment::Type::EllipticalArcTo: {
            auto& arc = static_cast<EllipticalArcSegment&>(segment);
            Painter::for_each_line_segment_on_elliptical_arc(cursor, arc.point(), arc.center(), arc.radii(), arc.x_axis_rotation(), arc.theta_1(), arc.theta_delta(), [&](const FloatPoint& p0, const FloatPoint& p1) {
                add_line(p0, p1);
            });
            cursor = segment.point();
            break;
        }
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
        }

        first = false;
    }

    // sort segments by ymax
    quick_sort(segments, [](const auto& line0, const auto& line1) {
        return line1.maximum_y < line0.maximum_y;
    });

    m_split_lines = move(segments);
    m_bounding_box = Gfx::FloatRect { min_x, min_y, max_x - min_x, max_y - min_y };
}

}
