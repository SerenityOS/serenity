/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLAnchorElementPrototype.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLAnchorElement::HTMLAnchorElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLAnchorElementPrototype>("HTMLAnchorElement"));

    activation_behavior = [this](auto const& event) {
        run_activation_behavior(event);
    };
}

HTMLAnchorElement::~HTMLAnchorElement() = default;

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

void HTMLAnchorElement::run_activation_behavior(Web::DOM::Event const&)
{
    // The activation behavior of an a element element given an event event is:

    // 1. If element has no href attribute, then return.
    if (href().is_empty())
        return;

    // 2. Let hyperlinkSuffix be null.
    Optional<String> hyperlink_suffix {};

    // FIXME: 3. If event's target is an img with an ismap attribute
    //        specified, then:
    //   3.1. Let x and y be 0.
    //
    //   3.2. If event's isTrusted attribute is initialized to true, then
    //   set x to the distance in CSS pixels from the left edge of the image
    //   to the location of the click, and set y to the distance in CSS
    //   pixels from the top edge of the image to the location of the click.
    //
    //   3.3. If x is negative, set x to 0.
    //
    //   3.4. If y is negative, set y to 0.
    //
    //   3.5. Set hyperlinkSuffix to the concatenation of U+003F (?), the
    //   value of x expressed as a base-ten integer using ASCII digits,
    //   U+002C (,), and the value of y expressed as a base-ten integer
    //   using ASCII digits.

    // FIXME: 4. If element has a download attribute, or if the user has
    // expressed a preference to download the hyperlink, then download the
    // hyperlink created by element given hyperlinkSuffix.

    // 5. Otherwise, follow the hyperlink created by element given
    // hyperlinkSuffix.
    follow_the_hyperlink(hyperlink_suffix);
}

}
