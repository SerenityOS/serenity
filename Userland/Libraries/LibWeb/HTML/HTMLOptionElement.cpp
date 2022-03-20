/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLOptionElement.h>

namespace Web::HTML {

HTMLOptionElement::HTMLOptionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLOptionElement::~HTMLOptionElement() = default;

void HTMLOptionElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::selected) {
        // Except where otherwise specified, when the element is created, its selectedness must be set to true
        // if the element has a selected attribute. Whenever an option element's selected attribute is added,
        // if its dirtiness is false, its selectedness must be set to true.
        if (!m_dirty)
            m_selected = true;
    }
}

void HTMLOptionElement::did_remove_attribute(FlyString const& name)
{
    HTMLElement::did_remove_attribute(name);

    if (name == HTML::AttributeNames::selected) {
        // Whenever an option element's selected attribute is removed, if its dirtiness is false, its selectedness must be set to false.
        if (!m_dirty)
            m_selected = false;
    }
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option-selected
void HTMLOptionElement::set_selected(bool selected)
{
    // On setting, it must set the element's selectedness to the new value, set its dirtiness to true, and then cause the element to ask for a reset.
    m_selected = selected;
    m_dirty = true;
    ask_for_a_reset();
}

// https://html.spec.whatwg.org/multipage/form-elements.html#ask-for-a-reset
void HTMLOptionElement::ask_for_a_reset()
{
    // FIXME: Implement this operation.
}

}
