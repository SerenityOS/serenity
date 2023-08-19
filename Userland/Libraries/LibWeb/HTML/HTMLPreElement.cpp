/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/HTML/HTMLPreElement.h>

namespace Web::HTML {

HTMLPreElement::HTMLPreElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLPreElement::~HTMLPreElement() = default;

void HTMLPreElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLPreElementPrototype>(realm, "HTMLPreElement"));
}

void HTMLPreElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    HTMLElement::apply_presentational_hints(style);

    for_each_attribute([&](auto const& name, auto const&) {
        if (name.equals_ignoring_ascii_case(HTML::AttributeNames::wrap))
            style.set_property(CSS::PropertyID::WhiteSpace, CSS::IdentifierStyleValue::create(CSS::ValueID::PreWrap));
    });
}

}
