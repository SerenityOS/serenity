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

#include <LibGfx/Painter.h>
#include <LibWeb/Layout/LayoutSVGPath.h>
#include <LibWeb/SVG/SVGPathElement.h>

namespace Web {

LayoutSVGPath::LayoutSVGPath(DOM::Document& document, SVG::SVGPathElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : LayoutSVGGraphics(document, element, properties)
{
}

void LayoutSVGPath::layout(LayoutNode::LayoutMode mode)
{
    auto& bounding_box = node().get_path().bounding_box();
    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    set_intrinsic_width(bounding_box.width());
    set_intrinsic_height(bounding_box.height());
    set_offset(bounding_box.top_left());
    LayoutSVGGraphics::layout(mode);
}

void LayoutSVGPath::paint(PaintContext& context, LayoutNode::PaintPhase phase)
{
    if (!is_visible())
        return;

    LayoutSVGGraphics::paint(context, phase);

    if (phase != LayoutNode::PaintPhase::Foreground)
        return;

    auto& path_element = node();
    auto& path = path_element.get_path();

    // We need to fill the path before applying the stroke, however the filled
    // path must be closed, whereas the stroke path may not necessary be closed.
    // Copy the path and close it for filling, but use the previous path for stroke
    auto closed_path = path;
    closed_path.close();

    // Fills are computed as though all paths are closed (https://svgwg.org/svg2-draft/painting.html#FillProperties)
    auto& painter = context.painter();
    auto& svg_context = context.svg_context();

    auto offset = (absolute_position() - effective_offset()).to_type<int>();

    painter.translate(offset);

    painter.fill_path(
        closed_path,
        path_element.fill_color().value_or(svg_context.fill_color()),
        Gfx::Painter::WindingRule::EvenOdd);
    painter.stroke_path(
        path,
        path_element.stroke_color().value_or(svg_context.stroke_color()),
        path_element.stroke_width().value_or(svg_context.stroke_width()));

    painter.translate(-offset);
}

}
