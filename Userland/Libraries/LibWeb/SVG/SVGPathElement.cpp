/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/ExtraMathConstants.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGPathElement.h>

namespace Web::SVG {

[[maybe_unused]] static void print_instruction(const PathInstruction& instruction)
{
    VERIFY(PATH_DEBUG);

    auto& data = instruction.data;

    switch (instruction.type) {
    case PathInstructionType::Move:
        dbgln("Move (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 2)
            dbgln("    x={}, y={}", data[i], data[i + 1]);
        break;
    case PathInstructionType::ClosePath:
        dbgln("ClosePath (absolute={})", instruction.absolute);
        break;
    case PathInstructionType::Line:
        dbgln("Line (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 2)
            dbgln("    x={}, y={}", data[i], data[i + 1]);
        break;
    case PathInstructionType::HorizontalLine:
        dbgln("HorizontalLine (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); ++i)
            dbgln("    x={}", data[i]);
        break;
    case PathInstructionType::VerticalLine:
        dbgln("VerticalLine (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); ++i)
            dbgln("    y={}", data[i]);
        break;
    case PathInstructionType::Curve:
        dbgln("Curve (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 6)
            dbgln("    (x1={}, y1={}, x2={}, y2={}), (x={}, y={})", data[i], data[i + 1], data[i + 2], data[i + 3], data[i + 4], data[i + 5]);
        break;
    case PathInstructionType::SmoothCurve:
        dbgln("SmoothCurve (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 4)
            dbgln("    (x2={}, y2={}), (x={}, y={})", data[i], data[i + 1], data[i + 2], data[i + 3]);
        break;
    case PathInstructionType::QuadraticBezierCurve:
        dbgln("QuadraticBezierCurve (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 4)
            dbgln("    (x1={}, y1={}), (x={}, y={})", data[i], data[i + 1], data[i + 2], data[i + 3]);
        break;
    case PathInstructionType::SmoothQuadraticBezierCurve:
        dbgln("SmoothQuadraticBezierCurve (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 2)
            dbgln("    x={}, y={}", data[i], data[i + 1]);
        break;
    case PathInstructionType::EllipticalArc:
        dbgln("EllipticalArc (absolute={})", instruction.absolute);
        for (size_t i = 0; i < data.size(); i += 7)
            dbgln("    (rx={}, ry={}) x-axis-rotation={}, large-arc-flag={}, sweep-flag={}, (x={}, y={})",
                data[i],
                data[i + 1],
                data[i + 2],
                data[i + 3],
                data[i + 4],
                data[i + 5],
                data[i + 6]);
        break;
    case PathInstructionType::Invalid:
        dbgln("Invalid");
        break;
    }
}

SVGPathElement::SVGPathElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGeometryElement(document, move(qualified_name))
{
}

void SVGPathElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGGeometryElement::parse_attribute(name, value);

    if (name == "d") {
        m_instructions = AttributeParser::parse_path_data(value);
        m_path.clear();
    }
}

Gfx::Path& SVGPathElement::get_path()
{
    if (m_path.has_value())
        return m_path.value();

    Gfx::Path path;
    PathInstructionType last_instruction = PathInstructionType::Invalid;

    for (auto& instruction : m_instructions) {
        // If the first path element uses relative coordinates, we treat them as absolute by making them relative to (0, 0).
        auto last_point = path.segments().is_empty() ? Gfx::FloatPoint { 0, 0 } : path.segments().last().point();

        auto& absolute = instruction.absolute;
        auto& data = instruction.data;

        if constexpr (PATH_DEBUG) {
            print_instruction(instruction);
        }

        bool clear_last_control_point = true;

        switch (instruction.type) {
        case PathInstructionType::Move: {
            Gfx::FloatPoint point = { data[0], data[1] };
            if (absolute) {
                path.move_to(point);
            } else {
                path.move_to(point + last_point);
            }
            break;
        }
        case PathInstructionType::ClosePath:
            path.close();
            break;
        case PathInstructionType::Line: {
            Gfx::FloatPoint point = { data[0], data[1] };
            if (absolute) {
                path.line_to(point);
            } else {
                path.line_to(point + last_point);
            }
            break;
        }
        case PathInstructionType::HorizontalLine: {
            if (absolute)
                path.line_to(Gfx::FloatPoint { data[0], last_point.y() });
            else
                path.line_to(Gfx::FloatPoint { data[0] + last_point.x(), last_point.y() });
            break;
        }
        case PathInstructionType::VerticalLine: {
            if (absolute)
                path.line_to(Gfx::FloatPoint { last_point.x(), data[0] });
            else
                path.line_to(Gfx::FloatPoint { last_point.x(), data[0] + last_point.y() });
            break;
        }
        case PathInstructionType::EllipticalArc: {
            double rx = data[0];
            double ry = data[1];
            double x_axis_rotation = double { data[2] } * M_DEG2RAD;
            double large_arc_flag = data[3];
            double sweep_flag = data[4];

            Gfx::FloatPoint next_point;

            if (absolute)
                next_point = { data[5], data[6] };
            else
                next_point = { data[5] + last_point.x(), data[6] + last_point.y() };

            path.elliptical_arc_to(next_point, { rx, ry }, x_axis_rotation, large_arc_flag != 0, sweep_flag != 0);
            break;
        }
        case PathInstructionType::QuadraticBezierCurve: {
            clear_last_control_point = false;

            Gfx::FloatPoint through = { data[0], data[1] };
            Gfx::FloatPoint point = { data[2], data[3] };

            if (absolute) {
                path.quadratic_bezier_curve_to(through, point);
                m_previous_control_point = through;
            } else {
                auto control_point = through + last_point;
                path.quadratic_bezier_curve_to(control_point, point + last_point);
                m_previous_control_point = control_point;
            }
            break;
        }
        case PathInstructionType::SmoothQuadraticBezierCurve: {
            clear_last_control_point = false;

            if (m_previous_control_point.is_null()
                || ((last_instruction != PathInstructionType::QuadraticBezierCurve) && (last_instruction != PathInstructionType::SmoothQuadraticBezierCurve))) {
                m_previous_control_point = last_point;
            }

            auto dx_end_control = last_point.dx_relative_to(m_previous_control_point);
            auto dy_end_control = last_point.dy_relative_to(m_previous_control_point);
            auto control_point = Gfx::FloatPoint { last_point.x() + dx_end_control, last_point.y() + dy_end_control };

            Gfx::FloatPoint end_point = { data[0], data[1] };

            if (absolute) {
                path.quadratic_bezier_curve_to(control_point, end_point);
            } else {
                path.quadratic_bezier_curve_to(control_point, end_point + last_point);
            }

            m_previous_control_point = control_point;
            break;
        }

        case PathInstructionType::Curve: {
            clear_last_control_point = false;

            Gfx::FloatPoint c1 = { data[0], data[1] };
            Gfx::FloatPoint c2 = { data[2], data[3] };
            Gfx::FloatPoint p2 = { data[4], data[5] };
            if (!absolute) {
                p2 += last_point;
                c1 += last_point;
                c2 += last_point;
            }
            path.cubic_bezier_curve_to(c1, c2, p2);

            m_previous_control_point = c2;
            break;
        }

        case PathInstructionType::SmoothCurve: {
            clear_last_control_point = false;

            if (m_previous_control_point.is_null()
                || ((last_instruction != PathInstructionType::Curve) && (last_instruction != PathInstructionType::SmoothCurve))) {
                m_previous_control_point = last_point;
            }

            auto reflected_previous_control_x = last_point.dx_relative_to(m_previous_control_point);
            auto reflected_previous_control_y = last_point.dy_relative_to(m_previous_control_point);
            Gfx::FloatPoint c1 = Gfx::FloatPoint { reflected_previous_control_x, reflected_previous_control_y };
            Gfx::FloatPoint c2 = { data[0], data[1] };
            Gfx::FloatPoint p2 = { data[2], data[3] };
            if (!absolute) {
                p2 += last_point;
                c1 += last_point;
                c2 += last_point;
            }
            path.cubic_bezier_curve_to(c1, c2, p2);

            m_previous_control_point = c2;
            break;
        }
        case PathInstructionType::Invalid:
            VERIFY_NOT_REACHED();
        }

        if (clear_last_control_point) {
            m_previous_control_point = Gfx::FloatPoint {};
        }
        last_instruction = instruction.type;
    }

    m_path = path;
    return m_path.value();
}

}
