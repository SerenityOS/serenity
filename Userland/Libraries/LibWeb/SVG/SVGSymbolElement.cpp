/*
 * Copyright (c) 2023, Preston Taylor <95388976+PrestonLTaylor@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/ShorthandStyleValue.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGSymbolElement.h>
#include <LibWeb/SVG/SVGUseElement.h>

namespace Web::SVG {

SVGSymbolElement::SVGSymbolElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, qualified_name)
{
}

void SVGSymbolElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGSymbolElementPrototype>(realm, "SVGSymbolElement"));
}

// https://svgwg.org/svg2-draft/struct.html#SymbolNotes
void SVGSymbolElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    Base::apply_presentational_hints(style);

    if (is_direct_child_of_use_shadow_tree()) {
        // The generated instance of a ‘symbol’ that is the direct referenced element of a ‘use’ element must always have a computed value of inline for the display property.
        style.set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::Inline)));
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

JS::GCPtr<Layout::Node> SVGSymbolElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::Box>(document(), this, move(style));
}

}
