/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLAreaElement::HTMLAreaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLAreaElement"));
}

HTMLAreaElement::~HTMLAreaElement() = default;

void HTMLAreaElement::parse_attribute(FlyString const& name, DeprecatedString const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::href) {
        set_the_url();
    }
}

DeprecatedString HTMLAreaElement::hyperlink_element_utils_href() const
{
    return attribute(HTML::AttributeNames::href);
}

void HTMLAreaElement::set_hyperlink_element_utils_href(DeprecatedString href)
{
    MUST(set_attribute(HTML::AttributeNames::href, move(href)));
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLAreaElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

}
