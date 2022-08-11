/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ExtraMathConstants.h>
#include <LibWeb/HTML/Canvas/CanvasPath.h>

namespace Web::HTML {

void CanvasPath::close_path()
{
    m_path.close();
}

void CanvasPath::move_to(float x, float y)
{
    m_path.move_to({ x, y });
}

void CanvasPath::line_to(float x, float y)
{
    m_path.line_to({ x, y });
}

void CanvasPath::quadratic_curve_to(float cx, float cy, float x, float y)
{
    m_path.quadratic_bezier_curve_to({ cx, cy }, { x, y });
}

void CanvasPath::bezier_curve_to(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y)
{
    m_path.cubic_bezier_curve_to(Gfx::FloatPoint(cp1x, cp1y), Gfx::FloatPoint(cp2x, cp2y), Gfx::FloatPoint(x, y));
}

DOM::ExceptionOr<void> CanvasPath::arc(float x, float y, float radius, float start_angle, float end_angle, bool counter_clockwise)
{
    if (radius < 0)
        return DOM::IndexSizeError::create(String::formatted("The radius provided ({}) is negative.", radius));
    return ellipse(x, y, radius, radius, 0, start_angle, end_angle, counter_clockwise);
}

DOM::ExceptionOr<void> CanvasPath::ellipse(float x, float y, float radius_x, float radius_y, float rotation, float start_angle, float end_angle, bool counter_clockwise)
{
    if (radius_x < 0)
        return DOM::IndexSizeError::create(String::formatted("The major-axis radius provided ({}) is negative.", radius_x));

    if (radius_y < 0)
        return DOM::IndexSizeError::create(String::formatted("The minor-axis radius provided ({}) is negative.", radius_y));

    if (constexpr float tau = M_TAU; (!counter_clockwise && (end_angle - start_angle) >= tau)
        || (counter_clockwise && (start_angle - end_angle) >= tau)) {
        start_angle = 0;
        end_angle = tau;
    } else {
        start_angle = fmodf(start_angle, tau);
        end_angle = fmodf(end_angle, tau);
    }

    // Then, figure out where the ends of the arc are.
    // To do so, we can pretend that the center of this ellipse is at (0, 0),
    // and the whole coordinate system is rotated `rotation` radians around the x axis, centered on `center`.
    // The sign of the resulting relative positions is just whether our angle is on one of the left quadrants.
    auto sin_rotation = sinf(rotation);
    auto cos_rotation = cosf(rotation);

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
        float sn = sinf(angle) >= 0 ? 1 : -1;
        relative_x_position *= sn;
        relative_y_position *= sn;

        // Now rotate it (back) around the center point by 'rotation' radians, then move it back to our actual origin.
        auto relative_rotated_x_position = relative_x_position * cos_rotation - relative_y_position * sin_rotation;
        auto relative_rotated_y_position = relative_x_position * sin_rotation + relative_y_position * cos_rotation;
        return Gfx::FloatPoint { relative_rotated_x_position + x, relative_rotated_y_position + y };
    };

    auto start_point = resolve_point_with_angle(start_angle);
    auto end_point = resolve_point_with_angle(end_angle);

    m_path.move_to(start_point);

    double delta_theta = end_angle - start_angle;

    // FIXME: This is still goofy for some values.
    m_path.elliptical_arc_to(end_point, { radius_x, radius_y }, rotation, delta_theta > M_PI, !counter_clockwise);

    m_path.close();
    return {};
}

void CanvasPath::rect(float x, float y, float width, float height)
{
    m_path.move_to({ x, y });
    if (width == 0 || height == 0)
        return;
    m_path.line_to({ x + width, y });
    m_path.line_to({ x + width, y + height });
    m_path.line_to({ x, y + height });
    m_path.close();
}

}
