/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<SVGGraphicsPaintable> SVGGraphicsPaintable::create(Layout::SVGGraphicsBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<SVGGraphicsPaintable>(layout_box);
}

SVGGraphicsPaintable::SVGGraphicsPaintable(Layout::SVGGraphicsBox const& layout_box)
    : SVGPaintable(layout_box)
{
}

Layout::SVGGraphicsBox const& SVGGraphicsPaintable::layout_box() const
{
    return static_cast<Layout::SVGGraphicsBox const&>(layout_node());
}

void SVGGraphicsPaintable::before_children_paint(PaintContext& context, PaintPhase phase) const
{
    SVGPaintable::before_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;

    auto& graphics_element = layout_box().dom_node();

    if (auto fill_color = graphics_element.fill_color(); fill_color.has_value())
        context.svg_context().set_fill_color(*fill_color);
    if (auto stroke_color = graphics_element.stroke_color(); stroke_color.has_value())
        context.svg_context().set_stroke_color(*stroke_color);
    if (auto stroke_width = graphics_element.stroke_width(); stroke_width.has_value())
        context.svg_context().set_stroke_width(*stroke_width);
    if (auto fill_opacity = graphics_element.fill_opacity(); fill_opacity.has_value())
        context.svg_context().set_fill_opacity(*fill_opacity);
    if (auto stroke_opacity = graphics_element.stroke_opacity(); stroke_opacity.has_value())
        context.svg_context().set_stroke_opacity(*stroke_opacity);
}

}
