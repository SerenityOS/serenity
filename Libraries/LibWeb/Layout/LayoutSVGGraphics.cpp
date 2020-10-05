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

#include <LibWeb/Layout/LayoutSVGGraphics.h>

namespace Web {

LayoutSVGGraphics::LayoutSVGGraphics(DOM::Document& document, SVG::SVGGraphicsElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : LayoutSVG(document, element, properties)
{
}

void LayoutSVGGraphics::before_children_paint(PaintContext& context, LayoutNode::PaintPhase phase)
{
    LayoutSVG::before_children_paint(context, phase);
    if (phase != LayoutNode::PaintPhase::Foreground)
        return;

    auto& graphics_element = downcast<SVG::SVGGraphicsElement>(node());

    if (graphics_element.fill_color().has_value())
        context.svg_context().set_fill_color(graphics_element.fill_color().value());
    if (graphics_element.stroke_color().has_value())
        context.svg_context().set_stroke_color(graphics_element.stroke_color().value());
    if (graphics_element.stroke_width().has_value())
        context.svg_context().set_stroke_width(graphics_element.stroke_width().value());
}

void LayoutSVGGraphics::layout(LayoutNode::LayoutMode mode)
{
    LayoutReplaced::layout(mode);

    for_each_child([&](auto& child) { child.layout(mode); });
}

}
