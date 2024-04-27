/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/Bindings/HTMLAnchorElementPrototype.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/PixelUnits.h>
#include <LibWeb/ReferrerPolicy/ReferrerPolicy.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLAnchorElement);

HTMLAnchorElement::HTMLAnchorElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLAnchorElement::~HTMLAnchorElement() = default;

void HTMLAnchorElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLAnchorElement);
}

void HTMLAnchorElement::attribute_changed(FlyString const& name, Optional<String> const& value)
{
    HTMLElement::attribute_changed(name, value);
    if (name == HTML::AttributeNames::href) {
        set_the_url();
    }
}

Optional<String> HTMLAnchorElement::hyperlink_element_utils_href() const
{
    return attribute(HTML::AttributeNames::href);
}

WebIDL::ExceptionOr<void> HTMLAnchorElement::set_hyperlink_element_utils_href(String href)
{
    return set_attribute(HTML::AttributeNames::href, move(href));
}

bool HTMLAnchorElement::has_activation_behavior() const
{
    return true;
}

// https://html.spec.whatwg.org/multipage/links.html#links-created-by-a-and-area-elements
void HTMLAnchorElement::activation_behavior(Web::DOM::Event const& event)
{
    // The activation behavior of an a or area element element given an event event is:

    // 1. If element has no href attribute, then return.
    if (href().is_empty())
        return;

    // 2. Let hyperlinkSuffix be null.
    Optional<String> hyperlink_suffix {};

    // 3. If element is an a element, and event's target is an img with an ismap attribute specified, then:
    if (event.target() && is<HTMLImageElement>(*event.target()) && static_cast<HTMLImageElement const&>(*event.target()).has_attribute(AttributeNames::ismap)) {
        // 1. Let x and y be 0.
        CSSPixels x { 0 };
        CSSPixels y { 0 };

        // 2. If event's isTrusted attribute is initialized to true, then set x to the distance in CSS pixels from the left edge of the image
        //    to the location of the click, and set y to the distance in CSS pixels from the top edge of the image to the location of the click.
        if (event.is_trusted() && is<UIEvents::MouseEvent>(event)) {
            auto const& mouse_event = static_cast<UIEvents::MouseEvent const&>(event);
            x = CSSPixels { mouse_event.offset_x() };
            y = CSSPixels { mouse_event.offset_y() };
        }

        // 3. If x is negative, set x to 0.
        x = max(x, 0);

        // 4. If y is negative, set y to 0.
        y = max(y, 0);

        // 5. Set hyperlinkSuffix to the concatenation of U+003F (?), the value of x expressed as a base-ten integer using ASCII digits,
        //    U+002C (,), and the value of y expressed as a base-ten integer using ASCII digits.
        hyperlink_suffix = MUST(String::formatted("?{},{}", x.to_int(), y.to_int()));
    }

    // 4. Let userInvolvement be event's user navigation involvement.
    auto user_involvement = user_navigation_involvement(event);

    // 5. If the user has expressed a preference to download the hyperlink, then set userInvolvement to "browser UI".
    //    NOTE: That is, if the user has expressed a specific preference for downloading, this no longer counts as merely "activation".
    // Not applicable, see do_manual_download

    // 6. If element has a download attribute, or if the user has expressed a preference to download the
    //    hyperlink, then download the hyperlink created by element with hyperlinkSuffix set to hyperlinkSuffix and
    //    userInvolvement set to userInvolvement.
    if (has_attribute(HTML::AttributeNames::download)) {
        // FIXME: Download the hyperlink
    } else
        // 7. Otherwise, follow the hyperlink created by element with hyperlinkSuffix set to hyperlinkSuffix and userInvolvement set to userInvolvement.
        follow_the_hyperlink(hyperlink_suffix, user_involvement);
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
    if (!href().is_empty())
        return ARIA::Role::link;
    // https://www.w3.org/TR/html-aria/#el-a
    return ARIA::Role::generic;
}

// https://html.spec.whatwg.org/multipage/text-level-semantics.html#dom-a-text
String HTMLAnchorElement::text() const
{
    // The text attribute's getter must return this element's descendant text content.
    return descendant_text_content();
}

// https://html.spec.whatwg.org/multipage/text-level-semantics.html#dom-a-text
void HTMLAnchorElement::set_text(String const& text)
{
    // The text attribute's setter must string replace all with the given value within this element.
    string_replace_all(text);
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

// https://html.spec.whatwg.org/multipage/links.html#links-created-by-a-and-area-elements
void HTMLAnchorElement::do_manual_download()
{
    // NOTE: Performing a manual download of an anchor uses the same activation behavior algorithm, but does not create an event relevant for it

    // The activation behavior of an a or area element element given an event event is:

    // 1. If element has no href attribute, then return.
    if (href().is_empty())
        return;

    // 2. Let hyperlinkSuffix be null.
    Optional<String> hyperlink_suffix {};

    // 3. If element is an a element, and event's target is an img with an ismap attribute specified, then:
    // Not applicable: Attempting to perform a manual download on an img triggers seperate logic

    // 4. Let userInvolvement be event's user navigation involvement.
    // Not applicable

    // 5. If the user has expressed a preference to download the hyperlink, then set userInvolvement to "browser UI".
    //    NOTE: That is, if the user has expressed a specific preference for downloading, this no longer counts as merely "activation".
    [[maybe_unused]] auto user_involvement = UserNavigationInvolvement::BrowserUI;

    // 6. If element has a download attribute, or if the user has expressed a preference to download the
    //    hyperlink, then download the hyperlink created by element with hyperlinkSuffix set to hyperlinkSuffix and
    //    userInvolvement set to userInvolvement.
    // FIXME: Download the hyperlink

    // 7. Otherwise, follow the hyperlink created by element with hyperlinkSuffix set to hyperlinkSuffix and userInvolvement set to userInvolvement.
    // Not applicable
}

}
