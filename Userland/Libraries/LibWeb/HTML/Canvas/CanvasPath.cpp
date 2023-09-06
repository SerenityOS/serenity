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
