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
    set_prototype(&window().cached_web_prototype("HTMLAreaElement"));
}

HTMLAreaElement::~HTMLAreaElement() = default;

void HTMLAreaElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::href) {
        set_the_url();
    }
}

String HTMLAreaElement::hyperlink_element_utils_href() const
{
    return attribute(HTML::AttributeNames::href);
}

void HTMLAreaElement::set_hyperlink_element_utils_href(String href)
{
    set_attribute(HTML::AttributeNames::href, move(href));
}

}
