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
    : HTMLElement(document, move(qualified_name))
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
            form()->submit_form(*this).release_value_but_fixme_should_propagate_errors();
            break;
        case TypeAttributeState::Reset:
            // Reset Button
            // Reset element's form owner.
            form()->reset_form();
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

HTMLButtonElement::~HTMLButtonElement() = default;

void HTMLButtonElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLButtonElementPrototype>(realm, "HTMLButtonElement"));
}

StringView HTMLButtonElement::type() const
{
    auto value = deprecated_attribute(HTML::AttributeNames::type);

#define __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(keyword, _) \
    if (value.equals_ignoring_ascii_case(#keyword##sv))    \
        return #keyword##sv;
    ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE

    // The missing value default and invalid value default are the Submit Button state.
    return "submit"sv;
}

HTMLButtonElement::TypeAttributeState HTMLButtonElement::type_state() const
{
    auto value = deprecated_attribute(HTML::AttributeNames::type);

#define __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(keyword, state) \
    if (value.equals_ignoring_ascii_case(#keyword##sv))        \
        return HTMLButtonElement::TypeAttributeState::state;
    ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE

    // The missing value default and invalid value default are the Submit Button state.
    return HTMLButtonElement::TypeAttributeState::Submit;
}

WebIDL::ExceptionOr<void> HTMLButtonElement::set_type(String const& type)
{
    return set_attribute(HTML::AttributeNames::type, type);
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLButtonElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

// https://html.spec.whatwg.org/multipage/forms.html#concept-submit-button
// https://html.spec.whatwg.org/multipage/form-elements.html#the-button-element:concept-submit-button
bool HTMLButtonElement::is_submit_button() const
{
    // If the type attribute is in the Submit Button state, the element is specifically a submit button.
    return type_state() == TypeAttributeState::Submit;
}

// https://html.spec.whatwg.org/multipage/form-elements.html#the-button-element:concept-fe-value
DeprecatedString HTMLButtonElement::value() const
{
    if (!has_attribute(AttributeNames::value))
        return DeprecatedString::empty();
    return deprecated_attribute(AttributeNames::value);
}

}
