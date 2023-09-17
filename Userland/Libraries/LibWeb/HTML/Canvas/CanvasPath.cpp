/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ExtraMathConstants.h>
#include <LibWeb/HTML/Canvas/CanvasPath.h>

namespace Web::HTML {

Gfx::AffineTransform CanvasPath::active_transform() const
{
    if (m_canvas_state.has_value())
        return m_canvas_state->drawing_state().transform;
    return {};
}

void CanvasPath::close_path()
{
    m_path.close();
}

void CanvasPath::move_to(float x, float y)
{
    m_path.move_to(active_transform().map(Gfx::FloatPoint { x, y }));
}

void CanvasPath::line_to(float x, float y)
{
    m_path.line_to(active_transform().map(Gfx::FloatPoint { x, y }));
}

void CanvasPath::quadratic_curve_to(float cx, float cy, float x, float y)
{
    auto transform = active_transform();
    m_path.quadratic_bezier_curve_to(transform.map(Gfx::FloatPoint { cx, cy }), transform.map(Gfx::FloatPoint { x, y }));
}

void CanvasPath::bezier_curve_to(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y)
{
    auto transform = active_transform();
    m_path.cubic_bezier_curve_to(
        transform.map(Gfx::FloatPoint { cp1x, cp1y }), transform.map(Gfx::FloatPoint { cp2x, cp2y }), transform.map(Gfx::FloatPoint { x, y }));
}

WebIDL::ExceptionOr<void> CanvasPath::arc(float x, float y, float radius, float start_angle, float end_angle, bool counter_clockwise)
{
    if (radius < 0)
        return WebIDL::IndexSizeError::create(m_self->realm(), MUST(String::formatted("The radius provided ({}) is negative.", radius)));
    return ellipse(x, y, radius, radius, 0, start_angle, end_angle, counter_clockwise);
}

WebIDL::ExceptionOr<void> CanvasPath::ellipse(float x, float y, float radius_x, float radius_y, float rotation, float start_angle, float end_angle, bool counter_clockwise)
{
    if (radius_x < 0)
        return WebIDL::IndexSizeError::create(m_self->realm(), MUST(String::formatted("The major-axis radius provided ({}) is negative.", radius_x)));

    if (radius_y < 0)
        return WebIDL::IndexSizeError::create(m_self->realm(), MUST(String::formatted("The minor-axis radius provided ({}) is negative.", radius_y)));

    if (constexpr float tau = M_TAU; (!counter_clockwise && (end_angle - start_angle) >= tau)
        || (counter_clockwise && (start_angle - end_angle) >= tau)) {
        start_angle = 0;
        // FIXME: elliptical_arc_to() incorrectly handles the case where the start/end points are very close.
        // So we slightly fudge the numbers here to correct for that.
        end_angle = tau * 0.9999f;
    } else {
        start_angle = fmodf(start_angle, tau);
        end_angle = fmodf(end_angle, tau);
    }

    // Then, figure out where the ends of the arc are.
    // To do so, we can pretend that the center of this ellipse is at (0, 0),
    // and the whole coordinate system is rotated `rotation` radians around the x axis, centered on `center`.
    // The sign of the resulting relative positions is just whether our angle is on one of the left quadrants.
    float sin_rotation;
    float cos_rotation;
    AK::sincos(rotation, sin_rotation, cos_rotation);

    auto resolve_point_with_angle = [&](float angle) {
        auto tan_relative = tanf(angle);
        auto tan2 = tan_relative * tan_relative;

        auto ab = radius_x * radius_y;
        auto a2 = radius_x * radius_x;
        auto b2 = radius_y * radius_y;
        auto sqrt = sqrtf(b2 + a2 * tan2);

        auto relative_x_position = ab / sqrt;
        auto relative_y_position = ab * tan_relative / sqrt;

        // Make sure to set the correct sign
        // -1 if 0 ≤ θ < 90° or 270°< θ ≤ 360°
        //  1 if 90° < θ< 270°
        float sn = cosf(angle) >= 0 ? 1 : -1;
        relative_x_position *= sn;
        relative_y_position *= sn;

        // Now rotate it (back) around the center point by 'rotation' radians, then move it back to our actual origin.
        auto relative_rotated_x_position = relative_x_position * cos_rotation - relative_y_position * sin_rotation;
        auto relative_rotated_y_position = relative_x_position * sin_rotation + relative_y_position * cos_rotation;
        return Gfx::FloatPoint { relative_rotated_x_position + x, relative_rotated_y_position + y };
    };

    auto start_point = resolve_point_with_angle(start_angle);
    auto end_point = resolve_point_with_angle(end_angle);

    auto delta_theta = end_angle - start_angle;

    auto transform = active_transform();
    m_path.move_to(transform.map(start_point));
    m_path.elliptical_arc_to(
        transform.map(Gfx::FloatPoint { end_point }),
        transform.map(Gfx::FloatSize { radius_x, radius_y }),
        rotation + transform.rotation(),
        delta_theta > AK::Pi<float>, !counter_clockwise);

    return {};
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-arcto
WebIDL::ExceptionOr<void> CanvasPath::arc_to(double x1, double y1, double x2, double y2, double radius)
{
    // 1. If any of the arguments are infinite or NaN, then return.
    if (!isfinite(x1) || !isfinite(y1) || !isfinite(x2) || !isfinite(y2) || !isfinite(radius))
        return {};

    // 2. Ensure there is a subpath for (x1, y1).
    auto transform = active_transform();
    m_path.ensure_subpath(transform.map(Gfx::FloatPoint { x1, y1 }));

    // 3. If radius is negative, then throw an "IndexSizeError" DOMException.
    if (radius < 0)
        return WebIDL::IndexSizeError::create(m_self->realm(), MUST(String::formatted("The radius provided ({}) is negative.", radius)));

    // 4. Let the point (x0, y0) be the last point in the subpath,
    //    transformed by the inverse of the current transformation matrix
    //    (so that it is in the same coordinate system as the points passed to the method).
    // Point (x0, y0)
    auto p0 = m_path.last_point();
    // Point (x1, y1)
    auto p1 = transform.map(Gfx::FloatPoint { x1, y1 });
    // Point (x2, y2)
    auto p2 = transform.map(Gfx::FloatPoint { x2, y2 });

    // 5. If the point (x0, y0) is equal to the point (x1, y1),
    //    or if the point (x1, y1) is equal to the point (x2, y2),
    //    or if radius is zero, then add the point (x1, y1) to the subpath,
    //    and connect that point to the previous point (x0, y0) by a straight line.
    if (p0 == p1 || p1 == p2 || radius == 0) {
        m_path.line_to(p1);
        return {};
    }

    auto v1 = Gfx::FloatVector2 { p0.x() - p1.x(), p0.y() - p1.y() };
    auto v2 = Gfx::FloatVector2 { p2.x() - p1.x(), p2.y() - p1.y() };
    auto cos_theta = v1.dot(v2) / (v1.length() * v2.length());
    // 6. Otherwise, if the points (x0, y0), (x1, y1), and (x2, y2) all lie on a single straight line,
    //    then add the point (x1, y1) to the subpath,
    //    and connect that point to the previous point (x0, y0) by a straight line.
    if (-1 == cos_theta || 1 == cos_theta) {
        m_path.line_to(p1);
        return {};
    }

    // 7. Otherwise, let The Arc be the shortest arc given by circumference of the circle that has radius radius,
    // and that has one point tangent to the half-infinite line that crosses the point (x0, y0) and ends at the point (x1, y1),
    // and that has a different point tangent to the half-infinite line that ends at the point (x1, y1) and crosses the point (x2, y2).
    // The points at which this circle touches these two lines are called the start and end tangent points respectively.
    auto adjacent = radius / static_cast<double>(tan(acos(cos_theta) / 2));
    auto factor1 = adjacent / static_cast<double>(v1.length());
    auto x3 = static_cast<double>(p1.x()) + factor1 * static_cast<double>(p0.x() - p1.x());
    auto y3 = static_cast<double>(p1.y()) + factor1 * static_cast<double>(p0.y() - p1.y());
    auto start_tangent = Gfx::FloatPoint { x3, y3 };

    auto factor2 = adjacent / static_cast<double>(v2.length());
    auto x4 = static_cast<double>(p1.x()) + factor2 * static_cast<double>(p2.x() - p1.x());
    auto y4 = static_cast<double>(p1.y()) + factor2 * static_cast<double>(p2.y() - p1.y());
    auto end_tangent = Gfx::FloatPoint { x4, y4 };

    // Connect the point (x0, y0) to the start tangent point by a straight line, adding the start tangent point to the subpath.
    m_path.line_to(start_tangent);

    bool const large_arc = false; // always small since tangent points define arc endpoints and lines meet at (x1, y1)
    auto cross_product = v1.x() * v2.y() - v1.y() * v2.x();
    bool const sweep = cross_product < 0; // right-hand rule, true means clockwise

    // and then connect the start tangent point to the end tangent point by The Arc, adding the end tangent point to the subpath.
    m_path.arc_to(end_tangent, radius, large_arc, sweep);
    return {};
}

void CanvasPath::rect(float x, float y, float width, float height)
{
    auto transform = active_transform();
    m_path.move_to(transform.map(Gfx::FloatPoint { x, y }));
    if (width == 0 || height == 0)
        return;
    m_path.line_to(transform.map(Gfx::FloatPoint { x + width, y }));
    m_path.line_to(transform.map(Gfx::FloatPoint { x + width, y + height }));
    m_path.line_to(transform.map(Gfx::FloatPoint { x, y + height }));
    m_path.close();
}

}
