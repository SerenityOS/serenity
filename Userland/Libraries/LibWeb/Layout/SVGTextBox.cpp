/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/Painting/SVGTextPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Layout {

SVGTextBox::SVGTextBox(DOM::Document& document, SVG::SVGTextPositioningElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

CSSPixelPoint SVGTextBox::viewbox_origin() const
{
    auto* svg_box = dom_node().first_ancestor_of_type<SVG::SVGSVGElement>();
    if (!svg_box || !svg_box->view_box().has_value())
        return { 0, 0 };
    return { svg_box->view_box().value().min_x, svg_box->view_box().value().min_y };
}

Optional<Gfx::AffineTransform> SVGTextBox::layout_transform() const
{
    // FIXME: Since text layout boxes are currently 0x0 it is not possible handle viewBox scaling here.
    auto& geometry_element = dom_node();
    auto transform = geometry_element.get_transform();
    auto* svg_box = geometry_element.first_ancestor_of_type<SVG::SVGSVGElement>();
    auto origin = viewbox_origin().to_type<float>();
    Gfx::FloatPoint paint_offset = {};
    if (svg_box && svg_box->view_box().has_value())
        paint_offset = svg_box->paintable_box()->absolute_rect().location().to_type<float>();
    return Gfx::AffineTransform {}.translate(paint_offset).translate(-origin).multiply(transform);
}

JS::GCPtr<Painting::Paintable> SVGTextBox::create_paintable() const
{
    return Painting::SVGTextPaintable::create(*this);
}

}
