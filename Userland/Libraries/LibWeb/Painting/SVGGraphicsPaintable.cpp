/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>

namespace Web::Painting {

SVGGraphicsPaintable::SVGGraphicsPaintable(Layout::SVGGraphicsBox const& layout_box)
    : SVGPaintable(layout_box)
{
}

Layout::SVGGraphicsBox const& SVGGraphicsPaintable::layout_box() const
{
    return static_cast<Layout::SVGGraphicsBox const&>(layout_node());
}

void SVGGraphicsPaintable::before_children_paint(PaintContext& context, PaintPhase phase, ShouldClipOverflow should_clip_overflow) const
{
    SVGPaintable::before_children_paint(context, phase, should_clip_overflow);
    if (phase != PaintPhase::Foreground)
        return;

    auto& graphics_element = layout_box().dom_node();

    if (graphics_element.fill_color().has_value())
        context.svg_context().set_fill_color(graphics_element.fill_color().value());
    if (graphics_element.stroke_color().has_value())
        context.svg_context().set_stroke_color(graphics_element.stroke_color().value());
    if (graphics_element.stroke_width().has_value())
        context.svg_context().set_stroke_width(graphics_element.stroke_width().value());
}

}
