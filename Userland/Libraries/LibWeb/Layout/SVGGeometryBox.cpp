/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGPathElement.h>

namespace Web::Layout {

SVGGeometryBox::SVGGeometryBox(DOM::Document& document, SVG::SVGGeometryElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

void SVGGeometryBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    SVGGraphicsBox::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& geometry_element = dom_node();
    auto& path = geometry_element.get_path();

    Gfx::AntiAliasingPainter painter { context.painter() };
    auto& svg_context = context.svg_context();

    auto offset = svg_context.svg_element_position();
    painter.translate(offset);

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
}

}
