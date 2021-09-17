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
    context.clear_svg_context();
}

}
