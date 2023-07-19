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

    auto& text_element = layout_box().dom_node();
    auto const* svg_element = text_element.shadow_including_first_ancestor_of_type<SVG::SVGSVGElement>();
    auto svg_element_rect = svg_element->paintable_box()->absolute_rect();

    Gfx::PainterStateSaver save_painter { painter };
    auto svg_context_offset = context.floored_device_point(svg_element_rect.location()).to_type<int>();
    painter.translate(svg_context_offset);

    auto const& dom_node = layout_box().dom_node();

    auto child_text_content = dom_node.child_text_content();

    auto transform = layout_box().layout_transform();
    if (!transform.has_value())
        return;

    // FIXME: Support arbitrary path transforms for fonts.
    // FIMXE: This assumes transform->x_scale() == transform->y_scale().
    auto& scaled_font = layout_node().scaled_font(static_cast<float>(context.device_pixels_per_css_pixel()) * transform->x_scale());

    Utf8View text_content { child_text_content };
    auto text_offset = context.floored_device_point(dom_node.get_offset().transformed(*transform).to_type<CSSPixels>());

    // FIXME: Once SVGFormattingContext does text layout this logic should move there.
    // https://svgwg.org/svg2-draft/text.html#TextAnchoringProperties
    switch (text_element.text_anchor().value_or(SVG::TextAnchor::Start)) {
    case SVG::TextAnchor::Start:
        // The rendered characters are aligned such that the start of the resulting rendered text is at the initial
        // current text position.
        break;
    case SVG::TextAnchor::Middle: {
        // The rendered characters are shifted such that the geometric middle of the resulting rendered text
        // (determined from the initial and final current text position before applying the text-anchor property)
        // is at the initial current text position.
        text_offset.translate_by(-scaled_font.width(text_content) / 2, 0);
        break;
    }
    case SVG::TextAnchor::End: {
        // The rendered characters are shifted such that the end of the resulting rendered text (final current text
        // position before applying the text-anchor property) is at the initial current text position.
        text_offset.translate_by(-scaled_font.width(text_content), 0);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    painter.draw_text_run(text_offset.to_type<int>(), text_content, scaled_font, layout_node().computed_values().fill()->as_color());
}

}
