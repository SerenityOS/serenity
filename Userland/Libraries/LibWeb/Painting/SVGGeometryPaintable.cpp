/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGeometryPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Painting {

JS::NonnullGCPtr<SVGGeometryPaintable> SVGGeometryPaintable::create(Layout::SVGGeometryBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<SVGGeometryPaintable>(layout_box);
}

SVGGeometryPaintable::SVGGeometryPaintable(Layout::SVGGeometryBox const& layout_box)
    : SVGGraphicsPaintable(layout_box)
{
}

Layout::SVGGeometryBox const& SVGGeometryPaintable::layout_box() const
{
    return static_cast<Layout::SVGGeometryBox const&>(layout_node());
}

void SVGGeometryPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    SVGGraphicsPaintable::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& geometry_element = layout_box().dom_node();

    Gfx::AntiAliasingPainter painter { context.painter() };
    auto& svg_context = context.svg_context();

    auto offset = svg_context.svg_element_position();
    painter.translate(offset);

    auto const* svg_element = geometry_element.first_ancestor_of_type<SVG::SVGSVGElement>();
    auto maybe_view_box = svg_element->view_box();

    context.painter().add_clip_rect(context.enclosing_device_rect(absolute_rect()).to_type<int>());

    Gfx::Path path = const_cast<SVG::SVGGeometryElement&>(geometry_element).get_path().copy_transformed(Gfx::AffineTransform {}.multiply(layout_box().layout_transform()));

    if (auto fill_color = geometry_element.fill_color().value_or(svg_context.fill_color()); fill_color.alpha() > 0) {
        // We need to fill the path before applying the stroke, however the filled
        // path must be closed, whereas the stroke path may not necessary be closed.
        // Copy the path and close it for filling, but use the previous path for stroke
        auto closed_path = path;
        closed_path.close();

        // Fills are computed as though all paths are closed (https://svgwg.org/svg2-draft/painting.html#FillProperties)
        painter.fill_path(
            closed_path,
            fill_color,
            Gfx::Painter::WindingRule::EvenOdd);
    }

    if (auto stroke_color = geometry_element.stroke_color().value_or(svg_context.stroke_color()); stroke_color.alpha() > 0) {
        painter.stroke_path(
            path,
            stroke_color,
            geometry_element.stroke_width().value_or(svg_context.stroke_width()));
    }

    painter.translate(-offset);
    context.painter().clear_clip_rect();
}

}
