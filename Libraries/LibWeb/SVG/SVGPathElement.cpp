/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Layout/LayoutSVGPath.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <ctype.h>

//#define PATH_DEBUG

namespace Web::SVG {

#ifdef PATH_DEBUG
static void print_instruction(const PathInstruction& instruction)
{
    auto& data = instruction.data;

    switch (instruction.type) {
    case PathInstructionType::Move:
        dbg() << "Move (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 2)
            dbg() << "    x=" << data[i] << ", y=" << data[i + 1];
        break;
    case PathInstructionType::ClosePath:
        dbg() << "ClosePath (absolute=" << instruction.absolute << ")";
        break;
    case PathInstructionType::Line:
        dbg() << "Line (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 2)
            dbg() << "    x=" << data[i] << ", y=" << data[i + 1];
        break;
    case PathInstructionType::HorizontalLine:
        dbg() << "HorizontalLine (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); ++i)
            dbg() << "    x=" << data[i];
        break;
    case PathInstructionType::VerticalLine:
        dbg() << "VerticalLine (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); ++i)
            dbg() << "    y=" << data[i];
        break;
    case PathInstructionType::Curve:
        dbg() << "Curve (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 6)
            dbg() << "    (x1=" << data[i] << ", y1=" << data[i + 1] << "), (x2=" << data[i + 2] << ", y2=" << data[i + 3] << "), (x=" << data[i + 4] << ", y=" << data[i + 5] << ")";
        break;
    case PathInstructionType::SmoothCurve:
        dbg() << "SmoothCurve (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 4)
            dbg() << "    (x2=" << data[i] << ", y2=" << data[i + 1] << "), (x=" << data[i + 2] << ", y=" << data[i + 3] << ")";
        break;
    case PathInstructionType::QuadraticBezierCurve:
        dbg() << "QuadraticBezierCurve (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 4)
            dbg() << "    (x1=" << data[i] << ", y1=" << data[i + 1] << "), (x=" << data[i + 2] << ", y=" << data[i + 3] << ")";
        break;
    case PathInstructionType::SmoothQuadraticBezierCurve:
        dbg() << "SmoothQuadraticBezierCurve (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 2)
            dbg() << "    x=" << data[i] << ", y=" << data[i + 1];
        break;
    case PathInstructionType::EllipticalArc:
        dbg() << "EllipticalArc (absolute=" << instruction.absolute << ")";
        for (size_t i = 0; i < data.size(); i += 7)
            dbg() << "    (rx=" << data[i] << ", ry=" << data[i + 1] << ") x-axis-rotation=" << data[i + 2] << ", large-arc-flag=" << data[i + 3] << ", sweep-flag=" << data[i + 4] << ", (x=" << data[i + 5] << ", y=" << data[i + 6] << ")";
        break;
    case PathInstructionType::Invalid:
        dbg() << "Invalid";
        break;
    }
}
#endif

PathDataParser::PathDataParser(const String& source)
    : m_source(source)
{
}

Vector<PathInstruction> PathDataParser::parse()
{
    parse_whitespace();
    while (!done())
        parse_drawto();
    if (!m_instructions.is_empty() && m_instructions[0].type != PathInstructionType::Move)
        ASSERT_NOT_REACHED();
    return m_instructions;
}

void PathDataParser::parse_drawto()
{
    if (match('M') || match('m')) {
        parse_moveto();
    } else if (match('Z') || match('z')) {
        parse_closepath();
    } else if (match('L') || match('l')) {
        parse_lineto();
    } else if (match('H') || match('h')) {
        parse_horizontal_lineto();
    } else if (match('V') || match('v')) {
        parse_vertical_lineto();
    } else if (match('C') || match('c')) {
        parse_curveto();
    } else if (match('S') || match('s')) {
        parse_smooth_curveto();
    } else if (match('Q') || match('q')) {
        parse_quadratic_bezier_curveto();
    } else if (match('T') || match('t')) {
        parse_smooth_quadratic_bezier_curveto();
    } else if (match('A') || match('a')) {
        parse_elliptical_arc();
    } else {
        dbg() << "PathDataParser::parse_drawto failed to match: '" << ch() << "'";
        TODO();
    }
}

void PathDataParser::parse_moveto()
{
    bool absolute = consume() == 'M';
    parse_whitespace();
    for (auto& pair : parse_coordinate_pair_sequence())
        m_instructions.append({ PathInstructionType::Move, absolute, pair });
}

void PathDataParser::parse_closepath()
{
    bool absolute = consume() == 'Z';
    parse_whitespace();
    m_instructions.append({ PathInstructionType::ClosePath, absolute, {} });
}

void PathDataParser::parse_lineto()
{
    bool absolute = consume() == 'L';
    parse_whitespace();
    for (auto& pair : parse_coordinate_pair_sequence())
        m_instructions.append({ PathInstructionType::Line, absolute, pair });
}

void PathDataParser::parse_horizontal_lineto()
{
    bool absolute = consume() == 'H';
    parse_whitespace();
    m_instructions.append({ PathInstructionType::HorizontalLine, absolute, parse_coordinate_sequence() });
}

void PathDataParser::parse_vertical_lineto()
{
    bool absolute = consume() == 'V';
    parse_whitespace();
    m_instructions.append({ PathInstructionType::VerticalLine, absolute, parse_coordinate_sequence() });
}

void PathDataParser::parse_curveto()
{
    bool absolute = consume() == 'C';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::Curve, absolute, parse_coordinate_pair_triplet() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void PathDataParser::parse_smooth_curveto()
{
    bool absolute = consume() == 'S';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::SmoothCurve, absolute, parse_coordinate_pair_double() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void PathDataParser::parse_quadratic_bezier_curveto()
{
    bool absolute = consume() == 'Q';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::QuadraticBezierCurve, absolute, parse_coordinate_pair_double() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void PathDataParser::parse_smooth_quadratic_bezier_curveto()
{
    bool absolute = consume() == 'T';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::SmoothQuadraticBezierCurve, absolute, parse_coordinate_pair() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void PathDataParser::parse_elliptical_arc()
{
    bool absolute = consume() == 'A';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::EllipticalArc, absolute, parse_elliptical_arg_argument() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

float PathDataParser::parse_coordinate()
{
    return parse_sign() * parse_number();
}

Vector<float> PathDataParser::parse_coordinate_pair()
{
    Vector<float> coordinates;
    coordinates.append(parse_coordinate());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.append(parse_coordinate());
    return coordinates;
}

Vector<float> PathDataParser::parse_coordinate_sequence()
{
    Vector<float> sequence;
    while (true) {
        sequence.append(parse_coordinate());
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_comma_whitespace() && !match_coordinate())
            break;
    }
    return sequence;
}

Vector<Vector<float>> PathDataParser::parse_coordinate_pair_sequence()
{
    Vector<Vector<float>> sequence;
    while (true) {
        sequence.append(parse_coordinate_pair());
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_comma_whitespace() && !match_coordinate())
            break;
    }
    return sequence;
}

Vector<float> PathDataParser::parse_coordinate_pair_double()
{
    Vector<float> coordinates;
    coordinates.append(parse_coordinate_pair());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.append(parse_coordinate_pair());
    return coordinates;
}

Vector<float> PathDataParser::parse_coordinate_pair_triplet()
{
    Vector<float> coordinates;
    coordinates.append(parse_coordinate_pair());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.append(parse_coordinate_pair());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.append(parse_coordinate_pair());
    return coordinates;
}

Vector<float> PathDataParser::parse_elliptical_arg_argument()
{
    Vector<float> numbers;
    numbers.append(parse_number());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_number());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_number());
    parse_comma_whitespace();
    numbers.append(parse_flag());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_flag());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_coordinate_pair());

    return numbers;
}

void PathDataParser::parse_whitespace(bool must_match_once)
{
    bool matched = false;
    while (!done() && match_whitespace()) {
        consume();
        matched = true;
    }

    ASSERT(!must_match_once || matched);
}

void PathDataParser::parse_comma_whitespace()
{
    if (match(',')) {
        consume();
        parse_whitespace();
    } else {
        parse_whitespace(1);
        if (match(','))
            consume();
        parse_whitespace();
    }
}

float PathDataParser::parse_fractional_constant()
{
    StringBuilder builder;
    bool floating_point = false;

    while (!done() && isdigit(ch()))
        builder.append(consume());

    if (match('.')) {
        floating_point = true;
        builder.append('.');
        consume();
        while (!done() && isdigit(ch()))
            builder.append(consume());
    } else {
        ASSERT(builder.length() > 0);
    }

    if (floating_point)
        return strtof(builder.to_string().characters(), nullptr);
    return builder.to_string().to_int().value();
}

float PathDataParser::parse_number()
{
    auto number = parse_fractional_constant();
    if (match('e') || match('E'))
        TODO();
    return number;
}

float PathDataParser::parse_flag()
{
    if (!match('0') && !match('1'))
        ASSERT_NOT_REACHED();
    return consume() - '0';
}

int PathDataParser::parse_sign()
{
    if (match('-')) {
        consume();
        return -1;
    }
    if (match('+'))
        consume();
    return 1;
}

bool PathDataParser::match_whitespace() const
{
    if (done())
        return false;
    char c = ch();
    return c == 0x9 || c == 0x20 || c == 0xa || c == 0xc || c == 0xd;
}

bool PathDataParser::match_comma_whitespace() const
{
    return match_whitespace() || match(',');
}

bool PathDataParser::match_coordinate() const
{
    return !done() && (isdigit(ch()) || ch() == '-' || ch() == '+' || ch() == '.');
}

SVGPathElement::SVGPathElement(DOM::Document& document, const FlyString& tag_name)
    : SVGGeometryElement(document, tag_name)
{
}

RefPtr<LayoutNode> SVGPathElement::create_layout_node(const CSS::StyleProperties* parent_style)
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    if (style->display() == CSS::Display::None)
        return nullptr;
    return adopt(*new LayoutSVGPath(document(), *this, move(style)));
}

void SVGPathElement::parse_attribute(const FlyString& name, const String& value)
{
    SVGGeometryElement::parse_attribute(name, value);

    if (name == "d")
        m_instructions = PathDataParser(value).parse();
}

Gfx::Path& SVGPathElement::get_path()
{
    if (m_path.has_value())
        return m_path.value();

    Gfx::Path path;

    for (auto& instruction : m_instructions) {
        auto& absolute = instruction.absolute;
        auto& data = instruction.data;

#ifdef PATH_DEBUG
        print_instruction(instruction);
#endif

        bool clear_last_control_point = true;

        switch (instruction.type) {
        case PathInstructionType::Move: {
            Gfx::FloatPoint point = { data[0], data[1] };
            if (absolute) {
                path.move_to(point);
            } else {
                ASSERT(!path.segments().is_empty());
                path.move_to(point + path.segments().last().point());
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
                ASSERT(!path.segments().is_empty());
                path.line_to(point + path.segments().last().point());
            }
            break;
        }
        case PathInstructionType::HorizontalLine: {
            ASSERT(!path.segments().is_empty());
            auto last_point = path.segments().last().point();
            if (absolute) {
                path.line_to(Gfx::FloatPoint { data[0], last_point.y() });
            } else {
                path.line_to(Gfx::FloatPoint { data[0] + last_point.x(), last_point.y() });
            }
            break;
        }
        case PathInstructionType::VerticalLine: {
            ASSERT(!path.segments().is_empty());
            auto last_point = path.segments().last().point();
            if (absolute) {
                path.line_to(Gfx::FloatPoint { last_point.x(), data[0] });
            } else {
                path.line_to(Gfx::FloatPoint { last_point.x(), data[0] + last_point.y() });
            }
            break;
        }
        case PathInstructionType::EllipticalArc: {
            double rx = data[0];
            double ry = data[1];
            double x_axis_rotation = data[2] * M_DEG2RAD;
            double large_arc_flag = data[3];
            double sweep_flag = data[4];

            double x_axis_rotation_c = cos(x_axis_rotation);
            double x_axis_rotation_s = sin(x_axis_rotation);

            auto& last_point = path.segments().last().point();

            Gfx::FloatPoint next_point;

            if (absolute) {
                next_point = { data[5], data[6] };
            } else {
                next_point = { data[5] + last_point.x(), data[6] + last_point.y() };
            }

            // Step 1 of out-of-range radii correction
            if (rx == 0.0 || ry == 0.0) {
                path.line_to(next_point);
                break;
            }

            // Step 2 of out-of-range radii correction
            if (rx < 0)
                rx *= -1.0;
            if (ry < 0)
                ry *= -1.0;

            // Find (cx, cy), theta_1, theta_delta
            // Step 1: Compute (x1', y1')
            auto x_avg = (last_point.x() - next_point.x()) / 2.0f;
            auto y_avg = (last_point.y() - next_point.y()) / 2.0f;
            auto x1p = x_axis_rotation_c * x_avg + x_axis_rotation_s * y_avg;
            auto y1p = -x_axis_rotation_s * x_avg + x_axis_rotation_c * y_avg;

            // Step 2: Compute (cx', cy')
            double x1p_sq = pow(x1p, 2.0);
            double y1p_sq = pow(y1p, 2.0);
            double rx_sq = pow(rx, 2.0);
            double ry_sq = pow(ry, 2.0);

            // Step 3 of out-of-range radii correction
            double lambda = x1p_sq / rx_sq + y1p_sq / ry_sq;
            double multiplier;

            if (lambda > 1.0) {
                auto lambda_sqrt = sqrt(lambda);
                rx *= lambda_sqrt;
                ry *= lambda_sqrt;
                multiplier = 0.0;
            } else {
                double numerator = rx_sq * ry_sq - rx_sq * y1p_sq - ry_sq * x1p_sq;
                double denominator = rx_sq * y1p_sq + ry_sq * x1p_sq;
                multiplier = sqrt(numerator / denominator);
            }

            if (large_arc_flag == sweep_flag)
                multiplier *= -1.0;

            double cxp = multiplier * rx * y1p / ry;
            double cyp = multiplier * -ry * x1p / rx;

            // Step 3: Compute (cx, cy) from (cx', cy')
            x_avg = (last_point.x() + next_point.x()) / 2.0f;
            y_avg = (last_point.y() + next_point.y()) / 2.0f;
            double cx = x_axis_rotation_c * cxp - x_axis_rotation_s * cyp + x_avg;
            double cy = x_axis_rotation_s * cxp + x_axis_rotation_c * cyp + y_avg;

            double theta_1 = atan2((y1p - cyp) / ry, (x1p - cxp) / rx);
            double theta_2 = atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);

            auto theta_delta = theta_2 - theta_1;

            if (sweep_flag == 0 && theta_delta > 0.0f) {
                theta_delta -= M_TAU;
            } else if (sweep_flag != 0 && theta_delta < 0) {
                theta_delta += M_TAU;
            }

            path.elliptical_arc_to(next_point, { cx, cy }, { rx, ry }, x_axis_rotation, theta_1, theta_delta);

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
                ASSERT(!path.segments().is_empty());
                auto last_point = path.segments().last().point();
                auto control_point = through + last_point;
                path.quadratic_bezier_curve_to(control_point, point + last_point);
                m_previous_control_point = control_point;
            }
            break;
        }
        case PathInstructionType::SmoothQuadraticBezierCurve: {
            clear_last_control_point = false;

            ASSERT(!path.segments().is_empty());
            auto last_point = path.segments().last().point();

            if (m_previous_control_point.is_null()) {
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

        case PathInstructionType::Curve:
        case PathInstructionType::SmoothCurve:
            // Instead of crashing the browser every time we come across an SVG
            // with these path instructions, let's just skip them
            continue;
        case PathInstructionType::Invalid:
            ASSERT_NOT_REACHED();
        }

        if (clear_last_control_point) {
            m_previous_control_point = Gfx::FloatPoint {};
        }
    }

    m_path = path;
    return m_path.value();
}

}
