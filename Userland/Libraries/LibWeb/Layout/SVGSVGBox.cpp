/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Layout {

SVGSVGBox::SVGSVGBox(DOM::Document& document, SVG::SVGSVGElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

void SVGSVGBox::before_children_paint(PaintContext& context, Painting::PaintPhase phase)
{
    if (phase != Painting::PaintPhase::Foreground)
        return;

    if (!context.has_svg_context())
        context.set_svg_context(SVGContext(m_paint_box->absolute_rect()));

    SVGGraphicsBox::before_children_paint(context, phase);
}

void SVGSVGBox::after_children_paint(PaintContext& context, Painting::PaintPhase phase)
{
    SVGGraphicsBox::after_children_paint(context, phase);
    if (phase != Painting::PaintPhase::Foreground)
        return;
    context.clear_svg_context();
}

}
