/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
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
}

JS::ThrowCompletionOr<void> SVGSVGElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGSVGElementPrototype>(realm, "SVGSVGElement"));

    return {};
}

JS::GCPtr<Layout::Node> SVGSVGElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::SVGSVGBox>(document(), *this, move(style));
}

void SVGSVGElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    // NOTE: Hack to ensure SVG unitless widths/heights are parsed even with <!DOCTYPE html>
    auto previous_quirks_mode = document().mode();
    const_cast<DOM::Document&>(document()).set_quirks_mode(DOM::QuirksMode::Yes);
    ScopeGuard reset_quirks_mode = [&] {
        const_cast<DOM::Document&>(document()).set_quirks_mode(previous_quirks_mode);
    };

    auto width_attribute = attribute(SVG::AttributeNames::width);
    auto parsing_context = CSS::Parser::ParsingContext { document() };
    if (auto width_value = parse_css_value(parsing_context, attribute(Web::HTML::AttributeNames::width), CSS::PropertyID::Width)) {
        style.set_property(CSS::PropertyID::Width, width_value.release_nonnull());
    } else if (width_attribute == "") {
        // If the `width` attribute is an empty string, it defaults to 100%.
        // This matches WebKit and Blink, but not Firefox. The spec is unclear.
        // FIXME: Figure out what to do here.
        style.set_property(CSS::PropertyID::Width, CSS::PercentageStyleValue::create(CSS::Percentage { 100 }));
    }

    // Height defaults to 100%
    auto height_attribute = attribute(SVG::AttributeNames::height);
    if (auto height_value = parse_css_value(parsing_context, attribute(Web::HTML::AttributeNames::height), CSS::PropertyID::Height)) {
        style.set_property(CSS::PropertyID::Height, height_value.release_nonnull());
    } else if (height_attribute == "") {
        // If the `height` attribute is an empty string, it defaults to 100%.
        // This matches WebKit and Blink, but not Firefox. The spec is unclear.
        // FIXME: Figure out what to do here.
        style.set_property(CSS::PropertyID::Height, CSS::PercentageStyleValue::create(CSS::Percentage { 100 }));
    }
}

void SVGSVGElement::parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGGraphicsElement::parse_attribute(name, value);

    if (name.equals_ignoring_ascii_case(SVG::AttributeNames::viewBox))
        m_view_box = try_parse_view_box(value);
}

}
