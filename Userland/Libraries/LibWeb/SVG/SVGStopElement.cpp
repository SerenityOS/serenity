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

void SVGStopElement::parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGElement::parse_attribute(name, value);
    if (name == SVG::AttributeNames::offset) {
        m_offset = AttributeParser::parse_number_percentage(value);
    }
}

void SVGStopElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    CSS::Parser::ParsingContext parsing_context { document() };
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_ascii_case("stop-color"sv)) {
            CSS::Parser::ParsingContext parsing_context { document() };
            if (auto stop_color = parse_css_value(parsing_context, value, CSS::PropertyID::StopColor)) {
                style.set_property(CSS::PropertyID::StopColor, stop_color.release_nonnull());
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

JS::NonnullGCPtr<SVGAnimatedNumber> SVGStopElement::offset() const
{
    TODO();
}

JS::ThrowCompletionOr<void> SVGStopElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGStopElementPrototype>(realm, "SVGStopElement"));

    return {};
}

}
