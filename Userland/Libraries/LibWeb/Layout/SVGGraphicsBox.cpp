/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGGraphicsBox.h>

namespace Web::Layout {

SVGGraphicsBox::SVGGraphicsBox(DOM::Document& document, SVG::SVGGraphicsElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGBox(document, element, properties)
{
}

void SVGGraphicsBox::before_children_paint(PaintContext& context, PaintPhase phase)
{
    SVGBox::before_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;

    auto& graphics_element = verify_cast<SVG::SVGGraphicsElement>(dom_node());

    if (graphics_element.fill_color().has_value())
        context.svg_context().set_fill_color(graphics_element.fill_color().value());
    if (graphics_element.stroke_color().has_value())
        context.svg_context().set_stroke_color(graphics_element.stroke_color().value());
    if (graphics_element.stroke_width().has_value())
        context.svg_context().set_stroke_width(graphics_element.stroke_width().value());
}

}
