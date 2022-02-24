/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

SVGSVGElement::SVGSVGElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, qualified_name)
{
}

RefPtr<Layout::Node> SVGSVGElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::SVGSVGBox(document(), *this, move(style)));
}

void SVGSVGElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    // Width defaults to 100%
    if (auto width_value = parse_html_length(document(), attribute("width"))) {
        style.set_property(CSS::PropertyID::Width, width_value.release_nonnull());
    } else {
        style.set_property(CSS::PropertyID::Width, CSS::PercentageStyleValue::create(CSS::Percentage { 100 }));
    }

    // Height defaults to 100%
    if (auto height_value = parse_html_length(document(), attribute("height"))) {
        style.set_property(CSS::PropertyID::Height, height_value.release_nonnull());
    } else {
        style.set_property(CSS::PropertyID::Height, CSS::PercentageStyleValue::create(CSS::Percentage { 100 }));
    }
}

void SVGSVGElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGGraphicsElement::parse_attribute(name, value);

    if (name.equals_ignoring_case(SVG::AttributeNames::viewBox))
        m_view_box = try_parse_view_box(value);
}

}
