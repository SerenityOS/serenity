/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGStopElement.h>

namespace Web::SVG {

SVGStopElement::SVGStopElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGElement(document, qualified_name)
{
}

void SVGStopElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGElement::attribute_changed(name, value);
    if (name == SVG::AttributeNames::offset) {
        m_offset = AttributeParser::parse_number_percentage(value);
    }
}

void SVGStopElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    CSS::Parser::ParsingContext parsing_context { document() };
    for_each_attribute([&](auto& name, auto& value) {
        CSS::Parser::ParsingContext parsing_context { document() };
        if (name.equals_ignoring_ascii_case("stop-color"sv)) {
            if (auto stop_color = parse_css_value(parsing_context, value, CSS::PropertyID::StopColor).release_value_but_fixme_should_propagate_errors()) {
                style.set_property(CSS::PropertyID::StopColor, stop_color.release_nonnull());
            }
        } else if (name.equals_ignoring_ascii_case("stop-opacity"sv)) {
            if (auto stop_opacity = parse_css_value(parsing_context, value, CSS::PropertyID::StopOpacity).release_value_but_fixme_should_propagate_errors()) {
                style.set_property(CSS::PropertyID::StopOpacity, stop_opacity.release_nonnull());
            }
        }
    });
}

Gfx::Color SVGStopElement::stop_color() const
{
    if (auto css_values = computed_css_values())
        return css_values->stop_color();
    return Color::Black;
}

float SVGStopElement::stop_opacity() const
{
    if (auto css_values = computed_css_values())
        return css_values->stop_opacity();
    return 1;
}

JS::NonnullGCPtr<SVGAnimatedNumber> SVGStopElement::offset() const
{
    TODO();
}

void SVGStopElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGStopElementPrototype>(realm, "SVGStopElement"));
}

}
