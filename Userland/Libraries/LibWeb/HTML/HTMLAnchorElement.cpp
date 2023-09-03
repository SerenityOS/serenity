/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/ReferrerPolicy/ReferrerPolicy.h>

namespace Web::HTML {

HTMLAnchorElement::HTMLAnchorElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    activation_behavior = [this](auto const& event) {
        run_activation_behavior(event);
    };
}

HTMLAnchorElement::~HTMLAnchorElement() = default;

void HTMLAnchorElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLAnchorElementPrototype>(realm, "HTMLAnchorElement"));
}

void HTMLAnchorElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::attribute_changed(name, value);
    if (name == HTML::AttributeNames::href) {
        set_the_url();
    }
}

DeprecatedString HTMLAnchorElement::hyperlink_element_utils_href() const
{
    return deprecated_attribute(HTML::AttributeNames::href);
}

WebIDL::ExceptionOr<void> HTMLAnchorElement::set_hyperlink_element_utils_href(DeprecatedString href)
{
    return set_attribute(HTML::AttributeNames::href, move(href));
}

void HTMLAnchorElement::run_activation_behavior(Web::DOM::Event const&)
{
    // The activation behavior of an a element element given an event event is:

    // 1. If element has no href attribute, then return.
    if (href().is_empty())
        return;

    // AD-HOC: follow_the_hyperlink currently doesn't navigate properly with javascript urls
    // EventHandler::handle_mouseup performs the navigation steps for javascript urls instead
    if (href().starts_with("javascript:"sv))
        return;

    // 2. Let hyperlinkSuffix be null.
    Optional<DeprecatedString> hyperlink_suffix {};

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

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLAnchorElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

Optional<ARIA::Role> HTMLAnchorElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-a-no-href
    if (!href().is_null())
        return ARIA::Role::link;
    // https://www.w3.org/TR/html-aria/#el-a
    return ARIA::Role::generic;
}

// https://html.spec.whatwg.org/multipage/text-level-semantics.html#dom-a-text
String HTMLAnchorElement::text() const
{
    // The text attribute's getter must return this element's descendant text content.
    return MUST(String::from_deprecated_string(descendant_text_content()));
}

// https://html.spec.whatwg.org/multipage/text-level-semantics.html#dom-a-text
void HTMLAnchorElement::set_text(String const& text)
{
    // The text attribute's setter must string replace all with the given value within this element.
    string_replace_all(text.to_deprecated_string());
}

// https://html.spec.whatwg.org/multipage/text-level-semantics.html#dom-a-referrerpolicy
StringView HTMLAnchorElement::referrer_policy() const
{
    // FIXME: This should probably be `Reflect` in the IDL.
    // The IDL attribute referrerPolicy must reflect the referrerpolicy content attribute, limited to only known values.
    auto maybe_policy_string = attribute(HTML::AttributeNames::referrerpolicy);
    if (!maybe_policy_string.has_value())
        return ""sv;

    auto maybe_policy = ReferrerPolicy::from_string(maybe_policy_string.value());
    if (!maybe_policy.has_value())
        return ""sv;

    return ReferrerPolicy::to_string(maybe_policy.value());
}

// https://html.spec.whatwg.org/multipage/text-level-semantics.html#dom-a-referrerpolicy
WebIDL::ExceptionOr<void> HTMLAnchorElement::set_referrer_policy(String const& referrer_policy)
{
    // The IDL attribute referrerPolicy must reflect the referrerpolicy content attribute, limited to only known values.
    return set_attribute(HTML::AttributeNames::referrerpolicy, referrer_policy);
}

}
