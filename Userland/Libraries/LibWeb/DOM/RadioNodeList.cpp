/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/RadioNodeListPrototype.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/RadioNodeList.h>
#include <LibWeb/HTML/HTMLInputElement.h>

namespace Web::DOM {

JS::NonnullGCPtr<RadioNodeList> RadioNodeList::create(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter)
{
    return realm.heap().allocate<RadioNodeList>(realm, realm, root, scope, move(filter));
}

RadioNodeList::RadioNodeList(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter)
    : LiveNodeList(realm, root, scope, move(filter))
{
}

RadioNodeList::~RadioNodeList() = default;

void RadioNodeList::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::RadioNodeListPrototype>(realm, "RadioNodeList"));
}

static HTML::HTMLInputElement const* radio_button(Node const& node)
{
    if (!is<HTML::HTMLInputElement>(node))
        return nullptr;

    auto const& input_element = verify_cast<HTML::HTMLInputElement>(node);
    if (input_element.type_state() != HTML::HTMLInputElement::TypeAttributeState::RadioButton)
        return nullptr;

    return &input_element;
}

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#dom-radionodelist-value
FlyString RadioNodeList::value() const
{
    // 1. Let element be the first element in tree order represented by the RadioNodeList object that is an input element whose type
    //    attribute is in the Radio Button state and whose checkedness is true. Otherwise, let it be null.
    auto* element = verify_cast<HTML::HTMLInputElement>(first_matching([](Node const& node) -> bool {
        auto const* button = radio_button(node);
        if (!button)
            return false;

        return button->checked();
    }));

    // 2. If element is null, return the empty string.
    if (!element)
        return String {};

    // 3. If element is an element with no value attribute, return the string "on".
    auto const value = element->get_attribute(HTML::AttributeNames::value);
    if (value.is_null())
        return "on"_fly_string;

    // 4. Otherwise, return the value of element's value attribute.
    return MUST(FlyString::from_deprecated_fly_string(value));
}

void RadioNodeList::set_value(FlyString const& value)
{
    HTML::HTMLInputElement* element = nullptr;

    auto deprecated_value = value.to_deprecated_fly_string();

    // 1. If the new value is the string "on": let element be the first element in tree order represented by the RadioNodeList object
    //    that is an input element whose type attribute is in the Radio Button state and whose value content attribute is either absent,
    //    or present and equal to the new value, if any. If no such element exists, then instead let element be null.
    if (value == "on"sv) {
        element = verify_cast<HTML::HTMLInputElement>(first_matching([&deprecated_value](auto const& node) {
            auto const* button = radio_button(node);
            if (!button)
                return false;

            auto const value = button->get_attribute(HTML::AttributeNames::value);
            return value.is_null() || value == deprecated_value;
        }));
    }
    // 2. Otherwise: let element be the first element in tree order represented by the RadioNodeList object that is an input element whose
    //   type attribute is in the Radio Button state and whose value content attribute is present and equal to the new value, if any. If
    //   no such element exists, then instead let element be null.
    else {
        element = verify_cast<HTML::HTMLInputElement>(first_matching([&deprecated_value](auto const& node) {
            auto const* button = radio_button(node);
            if (!button)
                return false;

            auto const value = button->get_attribute(HTML::AttributeNames::value);
            return !value.is_null() && value == deprecated_value;
        }));
    }

    // 3. If element is not null, then set its checkedness to true.
    if (element)
        element->set_checked(true);
}

}
