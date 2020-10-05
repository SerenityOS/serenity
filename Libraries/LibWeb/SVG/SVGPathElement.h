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

    SVGPathElement(DOM::Document&, const FlyString& tag_name);
    virtual ~SVGPathElement() override = default;

    virtual RefPtr<LayoutNode> create_layout_node(const CSS::StyleProperties* parent_style) override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    Gfx::Path& get_path();

private:
    Vector<PathInstruction> m_instructions;
    Gfx::FloatPoint m_previous_control_point = {};
    Optional<Gfx::Path> m_path;
};

}

AK_BEGIN_TYPE_TRAITS(Web::SVG::SVGPathElement)
static bool is_type(const Web::DOM::Node& node) { return node.is_svg_element() && downcast<Web::SVG::SVGElement>(node).local_name() == Web::SVG::TagNames::path; }
AK_END_TYPE_TRAITS()
