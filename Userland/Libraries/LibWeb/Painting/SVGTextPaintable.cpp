/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/SVGTextPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Painting {

JS::NonnullGCPtr<SVGTextPaintable> SVGTextPaintable::create(Layout::SVGTextBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<SVGTextPaintable>(layout_box);
}

SVGTextPaintable::SVGTextPaintable(Layout::SVGTextBox const& layout_box)
    : SVGGraphicsPaintable(layout_box)
{
}

void SVGTextPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    if (!layout_node().computed_values().fill().has_value())
        return;

    if (layout_node().computed_values().fill()->is_url()) {
        dbgln("FIXME: Using url() as fill is not supported for svg text");
        return;
    }

    SVGGraphicsPaintable::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& painter = context.painter();
    auto const& dom_node = layout_box().dom_node();
    auto paint_transform = computed_transforms().svg_to_device_pixels_transform(context);
    auto& scaled_font = layout_box().scaled_font(paint_transform.x_scale());
    auto text_rect = context.enclosing_device_rect(absolute_rect()).to_type<int>();
    auto text_contents = dom_node.text_contents();

    painter.draw_text_run(text_rect.bottom_left(), Utf8View { text_contents }, scaled_font, layout_node().computed_values().fill()->as_color(), text_rect);
}

}
