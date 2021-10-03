/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLAnchorElement.h>

namespace Web::HTML {

HTMLAnchorElement::HTMLAnchorElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLAnchorElement::~HTMLAnchorElement()
{
}

void HTMLAnchorElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::href) {
        set_the_url();
    }
}

String HTMLAnchorElement::hyperlink_element_utils_href() const
{
    return attribute(HTML::AttributeNames::href);
}

void HTMLAnchorElement::set_hyperlink_element_utils_href(String href)
{
    set_attribute(HTML::AttributeNames::href, move(href));
}

}
