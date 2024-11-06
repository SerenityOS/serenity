/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>
#include <AK/Math.h>
#include <AK/StringBuilder.h>
#include <AK/TypeCasts.h>
#include <LibGfx/BoundingBox.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibGfx/TextLayout.h>
#include <LibGfx/Vector2.h>

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
    FloatPoint last_point = this->last_point();

    // Step 1 of out-of-range radii correction
    if (rx == 0.0 || ry == 0.0) {
        append_segment<PathSegment::LineTo>(next_point);
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
        multiplier = AK::sqrt(AK::max(0., numerator) / denominator);
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

void Path::quad(FloatQuad const& quad)
{
    move_to(quad.p1());
    line_to(quad.p2());
    line_to(quad.p3());
    line_to(quad.p4());
    close();
}

void Path::rounded_rect(FloatRect const& rect, CornerRadius top_left, CornerRadius top_right, CornerRadius bottom_right, CornerRadius bottom_left)
{
    auto x = rect.x();
    auto y = rect.y();
    auto width = rect.width();
    auto height = rect.height();

    if (top_left)
        move_to({ x + top_left.horizontal_radius, y });
    else
        move_to({ x, y });

    if (top_right) {
        horizontal_line_to(x + width - top_right.horizontal_radius);
        elliptical_arc_to({ x + width, y + top_right.horizontal_radius }, { top_right.horizontal_radius, top_right.vertical_radius }, 0, false, true);
    } else {
        horizontal_line_to(x + width);
    }

    if (bottom_right) {
        vertical_line_to(y + height - bottom_right.vertical_radius);
        elliptical_arc_to({ x + width - bottom_right.horizontal_radius, y + height }, { bottom_right.horizontal_radius, bottom_right.vertical_radius }, 0, false, true);
    } else {
        vertical_line_to(y + height);
    }

    if (bottom_left) {
        horizontal_line_to(x + bottom_left.horizontal_radius);
        elliptical_arc_to({ x, y + height - bottom_left.vertical_radius }, { bottom_left.horizontal_radius, bottom_left.vertical_radius }, 0, false, true);
    } else {
        horizontal_line_to(x);
    }

    if (top_left) {
        vertical_line_to(y + top_left.vertical_radius);
        elliptical_arc_to({ x + top_left.horizontal_radius, y }, { top_left.horizontal_radius, top_left.vertical_radius }, 0, false, true);
    } else {
        vertical_line_to(y);
    }
}

void Path::text(Utf8View text, Font const& font)
{
    if (!is<ScaledFont>(font)) {
        // FIXME: This API only accepts Gfx::Font for ease of use.
        dbgln("Cannot path-ify bitmap fonts!");
        return;
    }

    auto& scaled_font = static_cast<ScaledFont const&>(font);
    for_each_glyph_position(
        last_point(), text, scaled_font, [&](DrawGlyphOrEmoji glyph_or_emoji) {
            if (glyph_or_emoji.has<DrawGlyph>()) {
                auto& glyph = glyph_or_emoji.get<DrawGlyph>();
                move_to(glyph.position);
                auto glyph_id = scaled_font.glyph_id_for_code_point(glyph.code_point);
                scaled_font.append_glyph_path_to(*this, glyph_id);
            }
        },
        IncludeLeftBearing::Yes);
}

Path Path::place_text_along(Utf8View text, Font const& font) const
{
    if (!is<ScaledFont>(font)) {
        // FIXME: This API only accepts Gfx::Font for ease of use.
        dbgln("Cannot path-ify bitmap fonts!");
        return {};
    }

    auto lines = split_lines();
    auto next_point_for_offset = [&, line_index = 0U, distance_along_path = 0.0f, last_line_length = 0.0f](float offset) mutable -> Optional<FloatPoint> {
        while (line_index < lines.size() && offset > distance_along_path) {
            last_line_length = lines[line_index++].length();
            distance_along_path += last_line_length;
        }
        if (offset > distance_along_path)
            return {};
        if (last_line_length > 1) {
            // If the last line segment was fairly long, compute the point in the line.
            float p = (last_line_length + offset - distance_along_path) / last_line_length;
            auto current_line = lines[line_index - 1];
            return current_line.a() + (current_line.b() - current_line.a()).scaled(p);
        }
        if (line_index >= lines.size())
            return {};
        return lines[line_index].a();
    };

    auto& scaled_font = static_cast<Gfx::ScaledFont const&>(font);
    Gfx::Path result_path;
    Gfx::for_each_glyph_position(
        {}, text, font, [&](Gfx::DrawGlyphOrEmoji glyph_or_emoji) {
            auto* glyph = glyph_or_emoji.get_pointer<Gfx::DrawGlyph>();
            if (!glyph)
                return;
            auto offset = glyph->position.x();
            auto width = font.glyph_width(glyph->code_point);
            auto start = next_point_for_offset(offset);
            if (!start.has_value())
                return;
            auto end = next_point_for_offset(offset + width);
            if (!end.has_value())
                return;
            // Find the angle between the start and end points on the path.
            auto delta = *end - *start;
            auto angle = AK::atan2(delta.y(), delta.x());
            Gfx::Path glyph_path;
            // Rotate the glyph then move it to start point.
            auto glyph_id = scaled_font.glyph_id_for_code_point(glyph->code_point);
            scaled_font.append_glyph_path_to(glyph_path, glyph_id);
            auto transform = Gfx::AffineTransform {}
                                 .translate(*start)
                                 .multiply(Gfx::AffineTransform {}.rotate_radians(angle))
                                 .multiply(Gfx::AffineTransform {}.translate({ 0, -scaled_font.pixel_metrics().ascent }));
            glyph_path = glyph_path.copy_transformed(transform);
            result_path.append_path(glyph_path);
        },
        Gfx::IncludeLeftBearing::Yes);
    return result_path;
}

void Path::close()
{
    // If there's no `moveto` starting this subpath assume the start is (0, 0).
    FloatPoint first_point_in_subpath = { 0, 0 };
    for (auto it = end(); it-- != begin();) {
        auto segment = *it;
        if (segment.command() == PathSegment::MoveTo) {
            first_point_in_subpath = segment.point();
            break;
        }
    }
    if (first_point_in_subpath != last_point())
        line_to(first_point_in_subpath);
    append_segment<PathSegment::ClosePath>();
}

void Path::close_all_subpaths()
{
    // This is only called before filling, not before stroking, so this doesn't have to insert ClosePath segments.
    auto it = begin();
    // Note: Get the end outside the loop as closing subpaths will move the end.
    auto end = this->end();
    while (it < end) {
        // If there's no `moveto` starting this subpath assume the start is (0, 0).
        FloatPoint first_point_in_subpath = { 0, 0 };
        auto segment = *it;
        if (segment.command() == PathSegment::MoveTo) {
            first_point_in_subpath = segment.point();
            ++it;
        }
        // Find the end of the current subpath.
        FloatPoint cursor = first_point_in_subpath;
        for (; it < end; ++it) {
            auto segment = *it;
            if (segment.command() == PathSegment::ClosePath)
                continue;
            if (segment.command() == PathSegment::MoveTo)
                break;
            cursor = segment.point();
        }
        // Close the subpath.
        if (first_point_in_subpath != cursor) {
            move_to(cursor);
            line_to(first_point_in_subpath);
        }
    }
}

ByteString Path::to_byte_string() const
{
    // Dumps this path as an SVG compatible string.
    StringBuilder builder;
    if (is_empty() || m_commands.first() != PathSegment::MoveTo)
        builder.append("M 0,0"sv);
    for (auto segment : *this) {
        if (!builder.is_empty())
            builder.append(' ');
        switch (segment.command()) {
        case PathSegment::MoveTo:
            builder.append('M');
            break;
        case PathSegment::LineTo:
            builder.append('L');
            break;
        case PathSegment::QuadraticBezierCurveTo:
            builder.append('Q');
            break;
        case PathSegment::CubicBezierCurveTo:
            builder.append('C');
            break;
        case PathSegment::ClosePath:
            builder.append('Z');
            break;
        }
        for (auto point : segment.points())
            builder.appendff(" {},{}", point.x(), point.y());
    }
    return builder.to_byte_string();
}

void Path::segmentize_path()
{
    Vector<FloatLine> segments;
    FloatBoundingBox bounding_box;
    Vector<size_t> subpath_end_indices;

    auto add_line = [&](auto const& p0, auto const& p1) {
        segments.append({ p0, p1 });
        bounding_box.add_point(p1);
    };

    FloatPoint cursor { 0, 0 };
    for (auto segment : *this) {
        switch (segment.command()) {
        case PathSegment::MoveTo:
            bounding_box.add_point(segment.point());
            break;
        case PathSegment::LineTo: {
            add_line(cursor, segment.point());
            break;
        }
        case PathSegment::QuadraticBezierCurveTo: {
            Painter::for_each_line_segment_on_bezier_curve(segment.through(), cursor, segment.point(), [&](FloatPoint p0, FloatPoint p1) {
                add_line(p0, p1);
            });
            break;
        }
        case PathSegment::CubicBezierCurveTo: {
            Painter::for_each_line_segment_on_cubic_bezier_curve(segment.through_0(), segment.through_1(), cursor, segment.point(), [&](FloatPoint p0, FloatPoint p1) {
                add_line(p0, p1);
            });
            break;
        }
        case PathSegment::ClosePath: {
            subpath_end_indices.append(segments.size() - 1);
            break;
        }
        }
        if (segment.command() != PathSegment::ClosePath)
            cursor = segment.point();
    }

    m_split_lines = SplitLines { move(segments), bounding_box, move(subpath_end_indices) };
}

Path Path::copy_transformed(Gfx::AffineTransform const& transform) const
{
    Path result;
    result.m_commands = m_commands;
    result.m_points.ensure_capacity(m_points.size());
    for (auto point : m_points)
        result.m_points.unchecked_append(transform.map(point));
    return result;
}

void Path::transform(AffineTransform const& transform)
{
    for (auto& point : m_points)
        point = transform.map(point);
}

void Path::append_path(Path const& path, AppendRelativeToLastPoint relative_to_last_point)
{
    auto previous_last_point = last_point();
    auto new_points_start = m_points.size();
    m_commands.extend(path.m_commands);
    m_points.extend(path.m_points);
    if (relative_to_last_point == AppendRelativeToLastPoint::Yes) {
        for (size_t i = new_points_start; i < m_points.size(); i++)
            m_points[i] += previous_last_point;
    }
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

static Vector<FloatPoint, 128> make_pen(float thickness)
{
    constexpr auto flatness = 0.15f;
    auto pen_vertex_count = 4;
    if (thickness > flatness) {
        pen_vertex_count = max(
            static_cast<int>(ceilf(AK::Pi<float>
                / acosf(1 - (2 * flatness) / thickness))),
            pen_vertex_count);
    }

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

    return pen_vertices;
}

static void apply_dash_pattern(Vector<Vector<FloatPoint>>& segments, Vector<bool>& segment_is_closed, Vector<float> dash_pattern, float dash_offset)
{
    VERIFY(!dash_pattern.is_empty());

    // Has to be ensured by callers. (They all double the list, but <canvas> needs to do that in a way that
    // is visible to JS accessors, so don't do it here.)
    VERIFY(dash_pattern.size() % 2 == 0);

    // This implementation is vaguely based on the <canvas> spec. One difference is that the <canvas> spec
    // modifies the path in place, while this implementation returns a new path. The spec is written in terms
    // of [start, end] intervals that are removed from the input path, while we have to instead add the
    // complement of those intervals to the output path. This is done by keeping track of the previous `end`
    // value and then filling in the gap between that and the current `start` value on every interval, and
    // at the end of each subpath.

    Vector<Vector<FloatPoint>> new_segments;

    // https://html.spec.whatwg.org/multipage/canvas.html#line-styles:dash-list-5
    // 7. Let `pattern width` be the concatenation of all the entries of style's dash list, in coordinate space units.
    // (NOTE: The spec means sum, not concatenation.)
    float pattern_width = 0;
    for (auto& entry : dash_pattern) {
        VERIFY(entry >= 0);
        pattern_width += entry;
    }

    // 8. For each subpath `subpath` in `path`, run the following substeps. These substeps mutate the subpaths in `path` in vivo.
    for (auto const& [subpath_index, subpath] : enumerate(segments)) {
        float end, last_end = 0;

        // 1. Let `subpath width` be the length of all the lines of `subpath`, in coordinate space units.
        float subpath_width = 0;
        for (size_t i = 0; i < subpath.size() - 1; i++)
            subpath_width += subpath[i].distance_from(subpath[i + 1]);

        // 2. Let `offset` be the value of style's lineDashOffset, in coordinate space units.
        float offset = dash_offset;

        // 3. While `offset` is greater than `pattern width`, decrement it by pattern width.
        //    While `offset` is less than zero, increment it by `pattern width`.
        // FIXME: Rewrite this using fmodf() in the future, once this has good test coverage.
        while (offset > pattern_width)
            offset -= pattern_width;
        while (offset < 0)
            offset += pattern_width;

        // 4. Define `L` to be a linear coordinate line defined along all lines in subpath, such that the start of the first line
        //    in the subpath is defined as coordinate 0, and the end of the last line in the subpath is defined as coordinate `subpath width`.
        float L = 0;
        size_t current_vertex_index = 0;

        auto next_L = [&]() -> float {
            return L + subpath[current_vertex_index].distance_from(subpath[current_vertex_index + 1]);
        };

        auto append_distinct = [](Vector<FloatPoint>& path, FloatPoint p) {
            if (path.is_empty() || path.last() != p)
                path.append(p);
        };

        auto skip_until = [&](float target_L) {
            while (next_L() < target_L) {
                L = next_L();
                current_vertex_index++;
            }
        };

        auto append_until = [&](Vector<FloatPoint>& new_subpath, float target_L) {
            while (next_L() < target_L) {
                L = next_L();
                current_vertex_index++;
                append_distinct(new_subpath, subpath[current_vertex_index]);
            }
        };

        auto append_lerp = [&](Vector<FloatPoint>& new_subpath, float target_L) {
            VERIFY(target_L >= L);
            VERIFY(target_L <= next_L());
            append_distinct(new_subpath, mix(subpath[current_vertex_index], subpath[current_vertex_index + 1], (target_L - L) / (next_L() - L)));
        };

        // 5. Let `position` be zero minus offset.
        float position = -offset;

        // 6. Let `index` be 0.
        size_t index = 0;

        // 7. Let `current state` be off (the other states being on and zero-on).
        // (NOTE: The mentioned "zero-on" state in the spec appears unused.)
        enum class State {
            Off,
            On,
        };
        State current_state = State::Off;

    dash_on:
        // 8. Dash on: Let `segment length` be the value of style's dash list's `index`th entry.
        float segment_length = dash_pattern[index];

        // 9. Increment `position` by `segment length`.
        position += segment_length;

        // 10. If `position` is greater than `subpath width`, then end these substeps for this subpath and start them again for the next subpath;
        //     if there are no more subpaths, then jump to the step labeled `convert` instead.
        if (position > subpath_width) {
            if (last_end < subpath_width) {
                // Fill from last_end to subpath_width.
                Vector<FloatPoint> new_subpath;

                skip_until(last_end);
                append_lerp(new_subpath, last_end);
                for (++current_vertex_index; current_vertex_index < subpath.size(); ++current_vertex_index)
                    append_distinct(new_subpath, subpath[current_vertex_index]);

                new_segments.append(move(new_subpath));
            }
            continue;
        }

        // 11. If `segment length` is nonzero, then let current state be on.
        if (segment_length != 0)
            current_state = State::On;

        // 12. Increment `index` by one.
        index++;

        // 13. Dash off: Let segment length be the value of style's dash list's `index`th entry.
        // (NOTE: The label "Dash off:" in the spec appears unused.)
        segment_length = dash_pattern[index];

        // 14. Let `start` be the offset `position` on L.
        float start = position;

        // 15. Increment `position` by `segment length`.
        position += segment_length;

        // 16. If `position` is less than zero, then jump to the step labeled `post-cut`.
        if (position < 0)
            goto post_cut;

        // 17. If `start` is less than zero, then let `start` be zero.
        if (start < 0)
            start = 0;

        // 18. If `position` is greater than `subpath width`, then let `end` be the offset `subpath width` on `L`. Otherwise, let `end` be the offset `position` on `L`.
        end = position > subpath_width ? subpath_width : position;

        // 19. Jump to the first appropriate step:
        //   If segment length is zero and current state is off
        //       Do nothing, just continue to the next step.
        //   If current state is off
        //       Cut the line on which `end` finds itself short at `end` and place a point there, cutting in two the subpath that it was in;
        //       remove all line segments, joins, points, and subpaths that are between `start` and `end`; and finally place a single point at
        //       `start` with no lines connecting to it.
        //       The point has a directionality for the purposes of drawing line caps (see below). The directionality is the direction that
        //       the original line had at that point (i.e. when `L` was defined above).
        //   Otherwise
        //       Cut the line on which `start` finds itself into two at `start` and place a point there, cutting in two the subpath that it was in,
        //       and similarly cut the line on which `end` finds itself short at end and place a point there, cutting in two the subpath that it was in,
        //       and then remove all line segments, joins, points, and subpaths that are between `start` and `end`.
        if (segment_length == 0 && current_state == State::Off) {
            // Do nothing.
        } else if (current_state == State::Off) {
            Vector<FloatPoint> new_subpath;

            skip_until(start);
            append_lerp(new_subpath, start);

            // FIXME: Store directionality.
            new_segments.append(move(new_subpath));
        } else {
            Vector<FloatPoint> new_subpath;

            skip_until(last_end);
            append_lerp(new_subpath, last_end);
            append_until(new_subpath, start);
            append_lerp(new_subpath, start);

            new_segments.append(move(new_subpath));
            last_end = end;
        }

        // 20. If start and end are the same point, then this results in just the line being cut in two and two points being inserted there,
        //     with nothing being removed, unless a join also happens to be at that point, in which case the join must be removed.
        // FIXME: Not clear if we have to do anything here, given our inverted interval implementation.

    post_cut:
        // 21. Post-cut: If position is greater than subpath width, then jump to the step labeled convert.
        if (position > subpath_width)
            break;

        // 22. If segment length is greater than zero, then let positioned-at-on-dash be false.
        // (NOTE: The spec doesn't mention positioned-at-on-dash anywhere else.)

        // 23. Increment index by one. If it is equal to the number of entries in style's dash list, then let index be 0.
        index++;
        if (index == dash_pattern.size())
            index = 0;

        // 24. Return to the step labeled `dash on`.
        goto dash_on;
    }

    segments = move(new_segments);

    // This function is only called if there are dashes, and dashes are never closed.
    segment_is_closed.resize(segments.size());
    for (auto& is_closed : segment_is_closed)
        is_closed = false;
}

Path Path::stroke_to_fill(StrokeStyle const& style) const
{
    // Note: This convolves a polygon with the path using the algorithm described
    // in https://keithp.com/~keithp/talks/cairo2003.pdf (3.1 Stroking Splines via Convolution)
    // Cap style handling is done by replacing the convolution with an explicit shape
    // at the path's ends, but we still maintain a position on the pen and pretend we're convolving.

    auto thickness = style.thickness;
    auto cap_style = style.cap_style;
    auto join_style = style.join_style;

    VERIFY(thickness > 0);

    auto lines = split_lines();
    if (lines.is_empty())
        return Path {};

    auto subpath_end_indices = split_lines_subbpath_end_indices();

    // Paths can be disconnected, which a pain to deal with, so split it up.
    // Also filter out duplicate points here (but keep one-point paths around
    // since we draw round and square caps for them).
    Vector<Vector<FloatPoint>> segments;
    Vector<bool> segment_is_closed;
    segments.append({ lines.first().a() });
    for (auto const& [line_index, line] : enumerate(lines)) {
        if (line.a() == segments.last().last()) {
            if (line.a() != line.b())
                segments.last().append(line.b());
        } else {
            if (subpath_end_indices.size() >= segments.size())
                segment_is_closed.append(subpath_end_indices[segments.size() - 1] == line_index);
            else
                segment_is_closed.append(false);
            segments.append({ line.a() });
            if (line.a() != line.b())
                segments.last().append(line.b());
        }
    }
    if (segment_is_closed.size() < segments.size()) {
        if (subpath_end_indices.size() >= segments.size())
            segment_is_closed.append(subpath_end_indices[segments.size() - 1] == lines.size() - 1);
        else
            segment_is_closed.append(false);
        VERIFY(segment_is_closed.size() == segments.size());
    }

    if (!style.dash_pattern.is_empty())
        apply_dash_pattern(segments, segment_is_closed, style.dash_pattern, style.dash_offset);

    Vector<FloatPoint, 128> pen_vertices = make_pen(thickness);

    static constexpr auto mod = [](int a, int b) {
        VERIFY(b > 0);
        VERIFY(a + b >= 0);
        return (a + b) % b;
    };
    auto wrapping_index = [](auto& vertices, auto index) {
        return vertices[mod(index, vertices.size())];
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
    for (int i = 0; i < (int)pen_vertices.size(); i++) {
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

        auto angle = target_angle - current_angle;

        // If the end of the range is antiparallel to where we want to go,
        // we have to keep moving clockwise: In that case, the _next_ range
        // is what we want.
        if (fabs(angle - AK::Pi<float>) < 0.0001f)
            return true;

        return angle <= AK::Pi<float>;
    };

    Path convolution;
    for (auto const& [segment_index, segment] : enumerate(segments)) {
        if (segment.size() < 2) {
            // Draw round and square caps for single-point segments.
            // FIXME: THis is is a bit ad-hoc. It matches what most PDF engines do,
            // and matches what Chrome and Firefox (but not WebKit) do for canvas paths.
            if (cap_style == CapStyle::Round) {
                convolution.move_to(segment[0] + pen_vertices[0]);
                for (int i = 1; i < (int)pen_vertices.size(); i++)
                    convolution.line_to(segment[0] + pen_vertices[i]);
                convolution.close();
            } else if (cap_style == CapStyle::Square) {
                convolution.rect({ segment[0].translated(-thickness / 2, -thickness / 2), { thickness, thickness } });
            }
            continue;
        }

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
        int active = *active_ranges.find_first_index_if([&](auto& range) {
            return range.in_range(start_slope);
        });

        shape_idx = 1;

        auto add_round_join = [&](unsigned next_index) {
            add_vertex(shape[shape_idx] + pen_vertices[active]);
            auto slope_now = angle_between(shape[shape_idx], shape[next_index]);
            auto range = active_ranges[active];
            while (!range.in_range(slope_now)) {
                active = mod(active + (clockwise(slope_now, range.end) ? 1 : -1), pen_vertices.size());
                add_vertex(shape[shape_idx] + pen_vertices[active]);
                range = active_ranges[active];
            }
        };

        auto add_bevel_join = [&](unsigned next_index) {
            add_vertex(shape[shape_idx] + pen_vertices[active]);
            auto slope_now = angle_between(shape[shape_idx], shape[next_index]);
            auto range = active_ranges[active];
            auto last_active = active;
            while (!range.in_range(slope_now)) {
                last_active = active;
                active = mod(active + (clockwise(slope_now, range.end) ? 1 : -1), pen_vertices.size());
                range = active_ranges[active];
            }
            if (last_active != active)
                add_vertex(shape[shape_idx] + pen_vertices[active]);
        };

        auto add_miter_join = [&](unsigned next_index) {
            auto cross_product = [](FloatPoint const& p1, FloatPoint const& p2) {
                return p1.x() * p2.y() - p1.y() * p2.x();
            };

            auto segment1 = shape[shape_idx] - shape[shape_idx - 1];
            auto normal1 = FloatVector2(-segment1.y(), segment1.x()).normalized();
            auto offset1 = FloatPoint(normal1.x(), normal1.y()) * (thickness / 2);
            auto p1 = shape[shape_idx - 1] + offset1;

            auto segment2 = shape[next_index] - shape[shape_idx];
            auto normal2 = FloatVector2(-segment2.y(), segment2.x()).normalized();
            auto offset2 = FloatPoint(normal2.x(), normal2.y()) * (thickness / 2);
            auto p2 = shape[shape_idx] + offset2;

            auto denominator = cross_product(segment1, segment2);
            if (denominator == 0)
                return add_bevel_join(next_index);

            auto intersection = p1 + segment1 * cross_product(p2 - p1, segment2) / denominator;
            if (intersection.distance_from(shape[shape_idx]) / (thickness / 2) > style.miter_limit)
                return add_bevel_join(next_index);

            add_vertex(intersection);
            auto slope_now = angle_between(shape[shape_idx], shape[next_index]);
            auto range = active_ranges[active];
            while (!range.in_range(slope_now)) {
                active = mod(active + (clockwise(slope_now, range.end) ? 1 : -1), pen_vertices.size());
                range = active_ranges[active];
            }
        };

        auto add_linejoin = [&](unsigned next_index) {
            switch (join_style) {
            case JoinStyle::Miter:
                add_miter_join(next_index);
                break;
            case JoinStyle::Round:
                add_round_join(next_index);
                break;
            case JoinStyle::Bevel:
                add_bevel_join(next_index);
                break;
            }
        };

        auto trace_path_until_index = [&](size_t index) {
            while (shape_idx < index) {
                add_linejoin(shape_idx + 1);
                shape_idx++;
            }
        };

        auto add_linecap = [&]() {
            if (cap_style == CapStyle::Butt || cap_style == CapStyle::Square) {
                auto segment = shape[shape_idx] - shape[shape_idx - 1];
                auto segment_vector = FloatVector2(segment.x(), segment.y()).normalized();
                auto normal = FloatVector2(-segment_vector.y(), segment_vector.x());
                auto offset = FloatPoint(normal.x() * (thickness / 2), normal.y() * (thickness / 2));
                auto p1 = shape[shape_idx] + offset;
                auto p2 = shape[shape_idx] - offset;
                if (cap_style == CapStyle::Square) {
                    auto square_cap_offset = segment_vector * (thickness / 2);
                    p1.translate_by(square_cap_offset.x(), square_cap_offset.y());
                    p2.translate_by(square_cap_offset.x(), square_cap_offset.y());
                }

                add_vertex(p1);
                auto slope_now = slope();
                active = mod(active + pen_vertices.size() / 2, pen_vertices.size());
                if (!active_ranges[active].in_range(slope_now)) {
                    if (wrapping_index(active_ranges, active + 1).in_range(slope_now))
                        active = mod(active + 1, pen_vertices.size());
                    else if (wrapping_index(active_ranges, active - 1).in_range(slope_now))
                        active = mod(active - 1, pen_vertices.size());
                    else
                        VERIFY_NOT_REACHED();
                }
                add_vertex(p2);
                shape_idx++;
            } else {
                VERIFY(cap_style == CapStyle::Round);
                add_round_join(shape_idx + 1);
            }
        };

        bool current_segment_is_closed = segment_is_closed[segment_index];

        // Outer stroke.
        trace_path_until_index(segment.size() - 1);
        VERIFY(shape_idx == segment.size() - 1);

        // Close outer stroke for closed paths, or draw cap 1 for open paths.
        if (current_segment_is_closed) {
            add_linejoin(1);

            // Start an independent path for the inner stroke.
            convolution.close();
            first = true;

            auto start_slope = slope();
            active = *active_ranges.find_first_index_if([&](auto& range) {
                return range.in_range(start_slope);
            });

            ++shape_idx;
            VERIFY(shape_idx == segment.size());
        } else {
            add_linecap();
        }

        // Inner stroke.
        trace_path_until_index(2 * (segment.size() - 1));
        VERIFY(shape_idx == 2 * (segment.size() - 1));

        // Close inner stroke for closed paths, or draw cap 2 for open paths.
        if (current_segment_is_closed) {
            add_linejoin(segment.size());
        } else {
            add_linecap();
        }

        convolution.close();
    }

    return convolution;
}

}
