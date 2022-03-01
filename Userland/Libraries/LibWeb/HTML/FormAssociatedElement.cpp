/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLLegendElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>

namespace Web::HTML {

void FormAssociatedElement::set_form(HTMLFormElement* form)
{
    if (m_form)
        m_form->remove_associated_element({}, *this);
    m_form = form;
    if (m_form)
        m_form->add_associated_element({}, *this);
}

bool FormAssociatedElement::enabled() const
{
    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fe-disabled

    // A form control is disabled if any of the following conditions are met:
    // 1. The element is a button, input, select, textarea, or form-associated custom element, and the disabled attribute is specified on this element (regardless of its value).
    // FIXME: This doesn't check for form-associated custom elements.
    if ((is<HTMLButtonElement>(this) || is<HTMLInputElement>(this) || is<HTMLSelectElement>(this) || is<HTMLTextAreaElement>(this)) && has_attribute(HTML::AttributeNames::disabled))
        return false;

    // 2. The element is a descendant of a fieldset element whose disabled attribute is specified, and is not a descendant of that fieldset element's first legend element child, if any.
    auto* fieldset_ancestor = first_ancestor_of_type<HTMLFieldSetElement>();
    if (fieldset_ancestor && fieldset_ancestor->has_attribute(HTML::AttributeNames::disabled)) {
        auto* first_legend_element_child = fieldset_ancestor->first_child_of_type<HTMLLegendElement>();
        if (!first_legend_element_child || !is_descendant_of(*first_legend_element_child))
            return false;
    }

    return true;
}

}
