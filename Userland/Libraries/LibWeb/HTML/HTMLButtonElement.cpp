/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>

namespace Web::HTML {

HTMLButtonElement::HTMLButtonElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
{
    // https://html.spec.whatwg.org/multipage/form-elements.html#the-button-element:activation-behaviour
    activation_behavior = [this](auto&) {
        // 1. If element is disabled, then return.
        if (!enabled())
            return;

        // 2. If element does not have a form owner, then return.
        if (!form())
            return;

        // 3. If element's node document is not fully active, then return.
        if (!this->document().is_fully_active())
            return;

        // 4. Switch on element's type attribute's state:
        switch (type_state()) {
        case TypeAttributeState::Submit:
            // Submit Button
            // Submit element's form owner from element.
            form()->submit_form(this);
            break;
        case TypeAttributeState::Reset:
            // Reset Button
            // FIXME: Reset element's form owner.
            TODO();
            break;
        case TypeAttributeState::Button:
            // Button
            // Do nothing.
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };
}

HTMLButtonElement::~HTMLButtonElement()
{
}

String HTMLButtonElement::type() const
{
    auto value = attribute(HTML::AttributeNames::type);

#define __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(keyword, _) \
    if (value.equals_ignoring_case(#keyword))              \
        return #keyword;
    ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE

    // The missing value default and invalid value default are the Submit Button state.
    return "submit";
}

HTMLButtonElement::TypeAttributeState HTMLButtonElement::type_state() const
{
    auto value = attribute(HTML::AttributeNames::type);

#define __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(keyword, state) \
    if (value.equals_ignoring_case(#keyword))                  \
        return HTMLButtonElement::TypeAttributeState::state;
    ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE

    // The missing value default and invalid value default are the Submit Button state.
    return HTMLButtonElement::TypeAttributeState::Submit;
}

void HTMLButtonElement::set_type(String const& type)
{
    set_attribute(HTML::AttributeNames::type, type);
}

}
