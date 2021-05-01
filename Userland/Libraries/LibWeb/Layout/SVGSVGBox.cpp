/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGSVGBox.h>

namespace Web::Layout {

SVGSVGBox::SVGSVGBox(DOM::Document& document, SVG::SVGSVGElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

void SVGSVGBox::prepare_for_replaced_layout()
{
    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    set_intrinsic_width(dom_node().width());
    set_intrinsic_height(dom_node().height());
}

void SVGSVGBox::before_children_paint(PaintContext& context, PaintPhase phase)
{
    if (phase != PaintPhase::Foreground)
        return;

    if (!context.has_svg_context())
        context.set_svg_context(SVGContext());

    SVGGraphicsBox::before_children_paint(context, phase);
}

void SVGSVGBox::after_children_paint(PaintContext& context, PaintPhase phase)
{
    SVGGraphicsBox::after_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;
}

}
