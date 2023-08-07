/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/Painting/SVGGeometryPaintable.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Layout {

SVGGeometryBox::SVGGeometryBox(DOM::Document& document, SVG::SVGGeometryElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

CSSPixelPoint SVGGeometryBox::viewbox_origin() const
{
    auto* svg_box = dom_node().shadow_including_first_ancestor_of_type<SVG::SVGSVGElement>();
    if (!svg_box || !svg_box->view_box().has_value())
        return { 0, 0 };
    return { svg_box->view_box().value().min_x, svg_box->view_box().value().min_y };
}

Optional<Gfx::AffineTransform> SVGGeometryBox::layout_transform() const
{
    auto& geometry_element = dom_node();
    auto transform = geometry_element.get_transform();
    auto* svg_box = geometry_element.shadow_including_first_ancestor_of_type<SVG::SVGSVGElement>();
    double scaling = 1;
    auto origin = viewbox_origin().to_type<float>();
    Gfx::FloatPoint paint_offset = {};
    if (svg_box && geometry_element.view_box().has_value()) {
        // Note: SVGFormattingContext has already done the scaling based on the viewbox,
        // we now have to derive what it was from the original bounding box size.
        // FIXME: It would be nice if we could store the transform from layout somewhere, so we don't have to solve for it here.
        auto original_bounding_box = Gfx::AffineTransform {}.translate(-origin).multiply(transform).map(const_cast<SVG::SVGGeometryElement&>(geometry_element).get_path().bounding_box());
        float stroke_width = geometry_element.visible_stroke_width();
        original_bounding_box.inflate(stroke_width, stroke_width);
        // If the transform (or path) results in a empty box we can't display this.
        if (original_bounding_box.is_empty())
            return {};
        auto scaled_width = paintable_box()->content_width().to_double();
        auto scaled_height = paintable_box()->content_height().to_double();
        scaling = min(scaled_width / static_cast<double>(original_bounding_box.width()), scaled_height / static_cast<double>(original_bounding_box.height()));
        auto scaled_bounding_box = original_bounding_box.scaled(scaling, scaling);
        paint_offset = (paintable_box()->absolute_rect().location() - svg_box->paintable_box()->absolute_rect().location()).to_type<float>() - scaled_bounding_box.location();
    }
    return Gfx::AffineTransform {}.translate(paint_offset).scale(scaling, scaling).translate(-origin).multiply(transform);
}

JS::GCPtr<Painting::Paintable> SVGGeometryBox::create_paintable() const
{
    return Painting::SVGGeometryPaintable::create(*this);
}

}
