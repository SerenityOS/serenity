/*
 * Copyright (c) 2023, Preston Taylor <95388976+PrestonLTaylor@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/OverflowStyleValue.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGSymbolElement.h>
#include <LibWeb/SVG/SVGUseElement.h>

namespace Web::SVG {

SVGSymbolElement::SVGSymbolElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, qualified_name)
{
}

JS::ThrowCompletionOr<void> SVGSymbolElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGSymbolElementPrototype>(realm, "SVGSymbolElement"));

    return {};
}

// https://svgwg.org/svg2-draft/struct.html#SymbolNotes
void SVGSymbolElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    // The user agent style sheet sets the overflow property for ‘symbol’ elements to hidden.
    auto hidden = CSS::IdentifierStyleValue::create(CSS::ValueID::Hidden).release_value_but_fixme_should_propagate_errors();
    style.set_property(CSS::PropertyID::Overflow, CSS::OverflowStyleValue::create(hidden, hidden).release_value_but_fixme_should_propagate_errors());

    if (is_direct_child_of_use_shadow_tree()) {
        // The generated instance of a ‘symbol’ that is the direct referenced element of a ‘use’ element must always have a computed value of inline for the display property.
        style.set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::Inline)).release_value_but_fixme_should_propagate_errors());
    } else {
        // FIXME: When we have a DefaultSVG.css then use https://svgwg.org/svg2-draft/styling.html#UAStyleSheet instead.
        // The user agent must set the display property on the ‘symbol’ element to none, as part of the user agent style sheet,
        // and this declaration must have importance over any other CSS rule or presentation attribute.
        style.set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::None)).release_value_but_fixme_should_propagate_errors());
    }
}

void SVGSymbolElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    if (name.equals_ignoring_ascii_case(SVG::AttributeNames::viewBox))
        m_view_box = try_parse_view_box(value);
}

bool SVGSymbolElement::is_direct_child_of_use_shadow_tree() const
{
    auto maybe_shadow_root = parent();
    if (!is<DOM::ShadowRoot>(maybe_shadow_root)) {
        return false;
    }

    auto host = static_cast<const DOM::ShadowRoot&>(*maybe_shadow_root).host();
    return is<SVGUseElement>(host);
}

}
