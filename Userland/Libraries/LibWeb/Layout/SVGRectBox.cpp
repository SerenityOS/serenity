/*
 * Copyright (c) 2021, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Layout/SVGRectBox.h>
#include <LibWeb/SVG/SVGRectElement.h>

namespace Web::Layout {

SVGRectBox::SVGRectBox(DOM::Document& document, SVG::SVGRectElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

void SVGRectBox::prepare_for_replaced_layout()
{
}

void SVGRectBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    SVGGraphicsBox::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& rect_element = dom_node();

    auto& painter = context.painter();
    auto& svg_context = context.svg_context();

    auto offset = (absolute_position() - effective_offset()).to_type<int>();

    painter.translate(offset);

    auto width = (int)computed_values().width().raw_value();
    auto height = (int)computed_values().height().raw_value();

    painter.fill_rect(Gfx::IntRect {
                          rect_element.x, rect_element.y, width, height },
        rect_element.fill_color().value_or(svg_context.fill_color()));

    painter.translate(-offset);
}

}
