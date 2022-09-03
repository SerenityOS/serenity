/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLPreElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLPreElement::HTMLPreElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("HTMLPreElement"));
}

HTMLPreElement::~HTMLPreElement() = default;

void HTMLPreElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    HTMLElement::apply_presentational_hints(style);

    for_each_attribute([&](auto const& name, auto const&) {
        if (name.equals_ignoring_case(HTML::AttributeNames::wrap))
            style.set_property(CSS::PropertyID::WhiteSpace, CSS::IdentifierStyleValue::create(CSS::ValueID::PreWrap));
    });
}

}
