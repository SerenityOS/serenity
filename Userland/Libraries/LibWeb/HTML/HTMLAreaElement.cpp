/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLAreaElement::HTMLAreaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLAreaElement::~HTMLAreaElement() = default;

void HTMLAreaElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLAreaElementPrototype>(realm, "HTMLAreaElement"));
}

void HTMLAreaElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::attribute_changed(name, value);
    if (name == HTML::AttributeNames::href) {
        set_the_url();
    }
}

DeprecatedString HTMLAreaElement::hyperlink_element_utils_href() const
{
    return deprecated_attribute(HTML::AttributeNames::href);
}

WebIDL::ExceptionOr<void> HTMLAreaElement::set_hyperlink_element_utils_href(DeprecatedString href)
{
    return set_attribute(HTML::AttributeNames::href, move(href));
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLAreaElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

Optional<ARIA::Role> HTMLAreaElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-area-no-href
    if (!href().is_null())
        return ARIA::Role::link;
    // https://www.w3.org/TR/html-aria/#el-area
    return ARIA::Role::generic;
}

}
