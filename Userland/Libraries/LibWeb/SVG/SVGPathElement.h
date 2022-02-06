/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

enum class PathInstructionType {
    Move,
    ClosePath,
    Line,
    HorizontalLine,
    VerticalLine,
    Curve,
    SmoothCurve,
    QuadraticBezierCurve,
    SmoothQuadraticBezierCurve,
    EllipticalArc,
    Invalid,
};

struct PathInstruction {
    PathInstructionType type;
    bool absolute;
    Vector<float> data;
};

class PathDataParser final {
public:
    PathDataParser(const String& source);
    ~PathDataParser() = default;

    Vector<PathInstruction> parse();

private:
    void parse_drawto();

    void parse_moveto();
    void parse_closepath();
    void parse_lineto();
    void parse_horizontal_lineto();
    void parse_vertical_lineto();
    void parse_curveto();
    void parse_smooth_curveto();
    void parse_quadratic_bezier_curveto();
    void parse_smooth_quadratic_bezier_curveto();
    void parse_elliptical_arc();

    float parse_coordinate();
    Vector<float> parse_coordinate_pair();
    Vector<float> parse_coordinate_sequence();
    Vector<Vector<float>> parse_coordinate_pair_sequence();
    Vector<float> parse_coordinate_pair_double();
    Vector<float> parse_coordinate_pair_triplet();
    Vector<float> parse_elliptical_arg_argument();
    void parse_whitespace(bool must_match_once = false);
    void parse_comma_whitespace();
    float parse_fractional_constant();
    float parse_number();
    float parse_flag();
    // -1 if negative, +1 otherwise
    int parse_sign();

    bool match_whitespace() const;
    bool match_comma_whitespace() const;
    bool match_coordinate() const;
    bool match(char c) const { return !done() && ch() == c; }

    bool done() const { return m_cursor >= m_source.length(); }
    char ch() const { return m_source[m_cursor]; }
    char consume() { return m_source[m_cursor++]; }

    String m_source;
    size_t m_cursor { 0 };
    Vector<PathInstruction> m_instructions;
};

class SVGPathElement final : public SVGGeometryElement {
public:
    using WrapperType = Bindings::SVGPathElementWrapper;

    SVGPathElement(DOM::Document&, QualifiedName);
    virtual ~SVGPathElement() override = default;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    Gfx::Path& get_path();

private:
    Vector<PathInstruction> m_instructions;
    Gfx::FloatPoint m_previous_control_point = {};
    Optional<Gfx::Path> m_path;
};

}
