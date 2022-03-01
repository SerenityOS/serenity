/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLButtonElement.h>

namespace Web::HTML {

HTMLButtonElement::HTMLButtonElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
{
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
