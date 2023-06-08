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

void Path::elliptical_arc_to(FloatPoint point, FloatSize radii, double x_axis_rotation, bool large_arc, bool sweep)
{
    auto next_point = point;

    double rx = radii.width();
    double ry = radii.height();

    double x_axis_rotation_c = AK::cos(x_axis_rotation);
    double x_axis_rotation_s = AK::sin(x_axis_rotation);

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

    auto segment_count = m_segments.size();
    for (size_t i = 0; i < segment_count; i++) {
        // Note: We need to use m_segments[i] as append_segment() may invalidate any references.
        switch (m_segments[i]->type()) {
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
            result.elliptical_arc_to(
                transform.map(segment->point()),
                transform.map(arc_segment.center()),
                transform.map(arc_segment.radii()),
                arc_segment.x_axis_rotation() + transform.rotation(),
                arc_segment.theta_1(),
                arc_segment.theta_delta(),
                arc_segment.large_arc(),
                arc_segment.sweep());
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

Vector<Tuple<float, float>> Path::compute_dash_positions(float path_length, StrokeProperties const& stroke_properties) const
{
    // https://www.w3.org/TR/svg-strokes/#StrokeShape

    // FIXME: what about dashadjust???
    // FIXME: what about dashcorner???

    // Let pathlength be the length of the subpath.
    float pathlength = path_length;
    // Let dashes be the list of values of ‘stroke-dasharray’ on the element, converted to user
    // units, repeated if necessary so that it has an even number of elements; if the property
    // has the value none, then the list has a single value 0.
    // FIXME: Convert to user units
    Vector<float> dashes = stroke_properties.stroke_dasharray.size() > 0 ? stroke_properties.stroke_dasharray : (Vector<float>) { 0 };
    // Let count be the number of values in dashes.
    size_t count = dashes.size();
    // Let sum be the sum of the values in dashes.
    float sum = 0;
    for (float t : dashes)
        sum += t;
    // If sum = 0, then return a sequence with the single pair <0, pathlength>.
    if (sum == 0) {
        return { { 0, pathlength } };
    }
    // Let positions be an empty sequence.
    Vector<Tuple<float, float>> positions;
    // Let offset be the value of the ‘stroke-dashoffset’ property on the element.
    float offset = stroke_properties.stroke_dashoffset;
    // If offset is negative, then set offset to sum − abs(offset).
    if (offset < 0)
        offset = sum + offset;
    // Set offset to offset mod sum.
    offset = fmod(offset, sum);
    // Let index be the smallest integer such that sum(dashesi, 0 ≤ i ≤ index) ≥ offset.
    size_t index = 0;
    float local_sum = dashes[0];
    // NOTE: The condition index < dashes.size()-1 is not really needed.
    while (local_sum < offset and index < dashes.size() - 1) {
        local_sum += dashes[++index];
    }
    // Let dashlength be min(sum(dashesi, 0 ≤ i ≤ index) − offset, pathlength).
    float dashlength = min(local_sum - offset, pathlength);
    // If index mod 2 = 0, then append to positions the pair <0, dashlength>.
    if (index % 2 == 0)
        positions.append({ 0, dashlength });
    // Let position be dashlength.
    float position = dashlength;
    // While position < pathlength:
    while (position < pathlength) {
        // Set index to (index + 1) mod count.
        index = (index + 1) % count;
        // Let dashlength be min(dashesindex, pathlength − position).
        dashlength = min(dashes[index], pathlength - position);
        // If index mod 2 = 0, then append to positions the pair <position, position + dashlength>.
        if (index % 2 == 0)
            positions.append({ position, position + dashlength });
        // Set position to position + dashlength.
        position += dashlength;
    }
    // Return positions.
    return positions;
}

Path Path::stroke_to_fill(StrokeProperties const& stroke_properties) const
{
    // https://www.w3.org/TR/svg-strokes/#StrokeShape

    auto& lines = split_lines();
    if (lines.is_empty())
        return Path {};

    // Paths can be disconnected, which a pain to deal with, so split it up.
    Vector<Vector<Tuple<FloatPoint, float>>> segments;
    segments.append({ { lines.first().a(), 0 } });
    for (auto& line : lines) {
        if (line.a() == line.b()) // Remove 0 size lines. Probably not correct according to spec...
            continue;
        if (line.a() == segments.last().last().get<0>()) {
            segments.last().append({ line.b(), segments.last().last().get<1>() + line.length() });
        } else {
            segments.append({ { line.a(), 0 }, { line.b(), line.length() } });
        }
    }

    Path stroked_path;
    // 3. For each subpath of path:
    for (auto& subpath : segments) {
        // Let positions be the dash positions for the subpath.
        float path_length = subpath.last().get<1>();
        Vector<Tuple<float, float>> positions = compute_dash_positions(path_length, stroke_properties);
        // NOTE: positions are guaranteed to be in ascending order
        size_t start_index = 0;
        size_t line_count = subpath.size() - 1;
        // For each pair <start, end> in positions:
        for (auto p : positions) {
            float start = p.get<0>();
            float end = min(p.get<1>(), path_length);
            // Let dash be the shape that includes, for all distances between start and end along
            // the subpath, all points that lie on the line perpendicular to the subpath at that
            // distance and which are within distance ‘stroke-width’ of the point on the subpath at
            // that position.
            VERIFY(start - path_length < 1e-6f);
            VERIFY(end - path_length < 1e-6f);
            size_t line_index = start_index;
            while (subpath[line_index + 1].get<1>() - start < 1e-6f)
                line_index++;

            Vector<NonnullRefPtr<Segment const>> out_path;
            Vector<NonnullRefPtr<Segment const>> in_path;

            {
                // Add the first line
                float t = start - subpath[line_index].get<1>();
                FloatPoint A = subpath[line_index].get<0>();
                FloatPoint B = subpath[line_index + 1].get<0>();
                FloatVector2 AB = { B.x() - A.x(), B.y() - A.y() };
                AB.normalize();
                FloatVector2 AB_normal = { -AB.y(), AB.x() };
                AB_normal.normalize();
                AB_normal *= stroke_properties.stroke_width / 2;
                FloatPoint out = A + AB * t + AB_normal;
                FloatPoint in = A + AB * t - AB_normal;
                out_path.append(adopt_ref(*new MoveSegment(out)));
                switch (stroke_properties.stroke_linecap) {
                case StrokeProperties::StrokeLinecap::Butt:
                    in_path.append(adopt_ref(*new LineSegment(out)));
                    break;
                case StrokeProperties::StrokeLinecap::Round:
                    in_path.append(adopt_ref(*new EllipticalArcSegment(out, A + AB * t, { stroke_properties.stroke_width / 2, stroke_properties.stroke_width / 2 }, atan2(AB.x(), AB.y()), 0, AK::Pi<float>, false, false)));
                    break;
                case StrokeProperties::StrokeLinecap::Square:
                    in_path.append(adopt_ref(*new LineSegment(out)));
                    in_path.append(adopt_ref(*new LineSegment(out - AB * stroke_properties.stroke_width / 2)));
                    in_path.append(adopt_ref(*new LineSegment(in - AB * stroke_properties.stroke_width / 2)));
                    break;
                }
                in_path.append(adopt_ref(*new LineSegment(in)));
            }

            while (line_index + 1 < line_count and subpath[line_index + 1].get<1>() - end < 1e-6f) {
                // Add offseted line to path

                // FIXME: We need an algorithm for loop removal.
                // There is one in
                // Tiller, W.; Hanson, E.G. (1984). Offsets of Two-Dimensional Profiles. , 4(9), 0–46.
                // doi:10.1109/mcg.1984.275995
                // FIXME: We need a method to know when we change segment to act upon linejoin.
                // Alternatively we could use linejoin on every one of these small joins and it
                // would be correct. However I believe it is very computationally expensive.
                FloatPoint A = subpath[line_index].get<0>();
                FloatPoint B = subpath[line_index + 1].get<0>();
                FloatPoint C = subpath[line_index + 2].get<0>();

                FloatVector2 AB = { B.x() - A.x(), B.y() - A.y() };
                FloatVector2 BC = { C.x() - B.x(), C.y() - B.y() };

                FloatVector2 AB_normal = { -AB.y(), AB.x() };
                AB_normal.normalize();
                AB_normal *= stroke_properties.stroke_width / 2;
                FloatVector2 BC_normal = { -BC.y(), BC.x() };
                BC_normal.normalize();
                BC_normal *= stroke_properties.stroke_width / 2;

                FloatPoint P = A + AB_normal;
                FloatPoint Q = B + BC_normal;
                FloatVector2 PQ = { Q.x() - P.x(), Q.y() - P.y() };

                FloatPoint R = A - AB_normal;
                FloatPoint S = B - BC_normal;
                FloatVector2 RS = { S.x() - R.x(), S.y() - R.y() };

                float divisor = BC_normal.dot(AB);
                float t1 = fabs(divisor) < 0.001f ? 1 : BC_normal.dot(PQ) / divisor;
                float t2 = fabs(divisor) < 0.001f ? 1 : BC_normal.dot(RS) / divisor;

                FloatPoint out = P + AB * t1;
                FloatPoint in = R + AB * t2;
                out_path.append(adopt_ref(*new LineSegment(out)));
                in_path.append(adopt_ref(*new LineSegment(in)));

                line_index++;
            }

            {
                // Add the last line
                float t = end - subpath[line_index].get<1>();
                FloatPoint A = subpath[line_index].get<0>();
                FloatPoint B = subpath[line_index + 1].get<0>();
                FloatVector2 AB = { B.x() - A.x(), B.y() - A.y() };
                AB.normalize();
                FloatVector2 AB_normal = { -AB.y(), AB.x() };
                AB_normal.normalize();
                AB_normal *= stroke_properties.stroke_width / 2;

                FloatPoint out = A + AB * t + AB_normal;
                FloatPoint in = A + AB * t - AB_normal;
                out_path.append(adopt_ref(*new LineSegment(out)));
                switch (stroke_properties.stroke_linecap) {
                case StrokeProperties::StrokeLinecap::Butt:
                    out_path.append(adopt_ref(*new LineSegment(in)));
                    break;
                case StrokeProperties::StrokeLinecap::Round:
                    out_path.append(adopt_ref(*new EllipticalArcSegment(in, A + AB * t, { stroke_properties.stroke_width / 2, stroke_properties.stroke_width / 2 }, atan2(AB.x(), AB.y()), 0, -AK::Pi<float>, false, true)));
                    break;
                case StrokeProperties::StrokeLinecap::Square:
                    out_path.append(adopt_ref(*new LineSegment(out + AB * stroke_properties.stroke_width / 2)));
                    out_path.append(adopt_ref(*new LineSegment(in + AB * stroke_properties.stroke_width / 2)));
                    out_path.append(adopt_ref(*new LineSegment(in)));
                    break;
                }
            }
            start_index = line_index - 1;

            for (size_t i = 0; i < out_path.size(); i++) {
                stroked_path.m_segments.append(out_path[i]);
            }
            for (int i = in_path.size() - 1; i >= 0; i--) {
                stroked_path.m_segments.append(in_path[i]);
            }
        }
    }
    return stroked_path;
}
}
