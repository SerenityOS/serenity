/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/Painting/SVGSVGPaintable.h>

namespace Web::Layout {

SVGSVGBox::SVGSVGBox(DOM::Document& document, SVG::SVGSVGElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : ReplacedBox(document, element, move(properties))
{
}

JS::GCPtr<Painting::Paintable> SVGSVGBox::create_paintable() const
{
    return Painting::SVGSVGPaintable::create(*this);
}

void SVGSVGBox::prepare_for_replaced_layout()
{
    if (dom_node().has_attribute(HTML::AttributeNames::width) && dom_node().has_attribute(HTML::AttributeNames::height)) {
        Optional<CSSPixels> w;
        Optional<CSSPixels> h;
        auto parsing_context = CSS::Parser::ParsingContext { document() };
        auto width = parse_css_value(parsing_context, dom_node().attribute(Web::HTML::AttributeNames::width), CSS::PropertyID::Width);
        if (!width.is_null() && width->has_length())
            w = width->to_length().to_px(*this);

        auto height = parse_css_value(parsing_context, dom_node().attribute((HTML::AttributeNames::height)), CSS::PropertyID::Height);
        if (!height.is_null() && height->has_length())
            h = height->to_length().to_px(*this);
        if (w.has_value() && h.has_value()) {
            set_intrinsic_width(*w);
            set_intrinsic_height(*h);
            set_intrinsic_aspect_ratio(w->value() / h->value());
            return;
        }
    }

    Optional<CSSPixelRect> united_rect;

    auto add_to_united_rect = [&](CSSPixelRect const& rect) {
        if (united_rect.has_value())
            united_rect = united_rect->united(rect);
        else
            united_rect = rect;
    };

    for_each_in_subtree_of_type<SVGGeometryBox>([&](SVGGeometryBox const& geometry_box) {
        auto& dom_node = const_cast<SVG::SVGGeometryElement&>(geometry_box.dom_node());
        if (dom_node.has_attribute(HTML::AttributeNames::width) && dom_node.has_attribute(HTML::AttributeNames::height)) {
            CSSPixelRect rect;
            // FIXME: Allow for relative lengths here
            rect.set_width(computed_values().width().resolved(*this, CSS::Length::make_px(0)).to_px(*this));
            rect.set_height(computed_values().height().resolved(*this, CSS::Length::make_px(0)).to_px(*this));
            add_to_united_rect(rect);
            return IterationDecision::Continue;
        }

        auto& path = dom_node.get_path();
        auto path_bounding_box = path.bounding_box().to_type<CSSPixels>();

        // Stroke increases the path's size by stroke_width/2 per side.
        auto stroke_width = geometry_box.dom_node().stroke_width().value_or(0);
        path_bounding_box.inflate(stroke_width, stroke_width);

        auto& maybe_view_box = this->dom_node().view_box();

        if (maybe_view_box.has_value()) {
            auto view_box = maybe_view_box.value();
            CSSPixelRect rect(view_box.min_x, view_box.min_y, view_box.width, view_box.height);
            add_to_united_rect(rect);
            return IterationDecision::Continue;
        }

        add_to_united_rect(path_bounding_box);
        return IterationDecision::Continue;
    });

    if (united_rect.has_value()) {
        set_intrinsic_width(united_rect->width());
        set_intrinsic_height(united_rect->height());
        set_intrinsic_aspect_ratio(united_rect->width().value() / united_rect->height().value());
    }
}

}
