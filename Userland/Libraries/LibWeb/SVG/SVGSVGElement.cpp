/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Bindings/SVGSVGElementPrototype.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

SVGSVGElement::SVGSVGElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, qualified_name)
{
    set_prototype(&window().ensure_web_prototype<Bindings::SVGSVGElementPrototype>("SVGSVGElement"));
}

RefPtr<Layout::Node> SVGSVGElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::SVGSVGBox(document(), *this, move(style)));
}

void SVGSVGElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    auto width_attribute = attribute(SVG::AttributeNames::width);
    if (auto width_value = HTML::parse_dimension_value(width_attribute)) {
        style.set_property(CSS::PropertyID::Width, width_value.release_nonnull());
    } else if (width_attribute == "") {
        // If the `width` attribute is an empty string, it defaults to 100%.
        // This matches WebKit and Blink, but not Firefox. The spec is unclear.
        // FIXME: Figure out what to do here.
        style.set_property(CSS::PropertyID::Width, CSS::PercentageStyleValue::create(CSS::Percentage { 100 }));
    }

    // Height defaults to 100%
    auto height_attribute = attribute(SVG::AttributeNames::height);
    if (auto height_value = HTML::parse_dimension_value(height_attribute)) {
        style.set_property(CSS::PropertyID::Height, height_value.release_nonnull());
    } else if (height_attribute == "") {
        // If the `height` attribute is an empty string, it defaults to 100%.
        // This matches WebKit and Blink, but not Firefox. The spec is unclear.
        // FIXME: Figure out what to do here.
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
