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

#include <LibWeb/Layout/LayoutSVGSVG.h>

namespace Web {

LayoutSVGSVG::LayoutSVGSVG(DOM::Document& document, SVG::SVGSVGElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : LayoutSVGGraphics(document, element, properties)
{
}

void LayoutSVGSVG::layout(LayoutMode layout_mode)
{
    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    set_intrinsic_width(node().width());
    set_intrinsic_height(node().height());
    LayoutSVGGraphics::layout(layout_mode);
}

void LayoutSVGSVG::before_children_paint(PaintContext& context, LayoutNode::PaintPhase phase)
{
    if (phase != LayoutNode::PaintPhase::Foreground)
        return;

    if (!context.has_svg_context())
        context.set_svg_context(SVGContext());

    LayoutSVGGraphics::before_children_paint(context, phase);
}

void LayoutSVGSVG::after_children_paint(PaintContext& context, LayoutNode::PaintPhase phase)
{
    LayoutSVGGraphics::after_children_paint(context, phase);
    if (phase != LayoutNode::PaintPhase::Foreground)
        return;
}

}
