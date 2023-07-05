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

Optional<HitTestResult> SVGTextPaintable::hit_test(CSSPixelPoint position, HitTestType type) const
{
    (void)position;
    (void)type;
    return {};
}

void SVGTextPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    SVGGraphicsPaintable::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& painter = context.painter();

    auto& text_element = layout_box().dom_node();
    auto const* svg_element = text_element.shadow_including_first_ancestor_of_type<SVG::SVGSVGElement>();
    auto svg_element_rect = svg_element->paintable_box()->absolute_rect();

    Gfx::PainterStateSaver save_painter { painter };
    auto svg_context_offset = context.floored_device_point(svg_element_rect.location()).to_type<int>();
    painter.translate(svg_context_offset);

    auto const& dom_node = layout_box().dom_node();

    auto child_text_content = dom_node.child_text_content();

    auto transform = layout_box().layout_transform();
    auto& scaled_font = layout_node().scaled_font(context);
    auto text_offset = context.floored_device_point(dom_node.get_offset().transformed(*transform).to_type<CSSPixels>());
    painter.draw_text_run(text_offset.to_type<int>(), Utf8View { child_text_content }, scaled_font, layout_node().computed_values().fill()->as_color());
}

}
