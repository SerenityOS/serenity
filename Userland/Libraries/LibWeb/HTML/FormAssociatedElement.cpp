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
#include <LibWeb/HTML/Parser/HTMLParser.h>

namespace Web::HTML {

void FormAssociatedElement::set_form(HTMLFormElement* form)
{
    if (m_form)
        m_form->remove_associated_element({}, form_associated_element_to_html_element());
    m_form = form;
    if (m_form)
        m_form->add_associated_element({}, form_associated_element_to_html_element());
}

bool FormAssociatedElement::enabled() const
{
    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fe-disabled
    auto const& html_element = const_cast<FormAssociatedElement&>(*this).form_associated_element_to_html_element();

    // A form control is disabled if any of the following conditions are met:
    // 1. The element is a button, input, select, textarea, or form-associated custom element, and the disabled attribute is specified on this element (regardless of its value).
    // FIXME: This doesn't check for form-associated custom elements.
    if ((is<HTMLButtonElement>(html_element) || is<HTMLInputElement>(html_element) || is<HTMLSelectElement>(html_element) || is<HTMLTextAreaElement>(html_element)) && html_element.has_attribute(HTML::AttributeNames::disabled))
        return false;

    // 2. The element is a descendant of a fieldset element whose disabled attribute is specified, and is not a descendant of that fieldset element's first legend element child, if any.
    for (auto* fieldset_ancestor = html_element.first_ancestor_of_type<HTMLFieldSetElement>(); fieldset_ancestor; fieldset_ancestor = fieldset_ancestor->first_ancestor_of_type<HTMLFieldSetElement>()) {
        if (fieldset_ancestor->has_attribute(HTML::AttributeNames::disabled)) {
            auto* first_legend_element_child = fieldset_ancestor->first_child_of_type<HTMLLegendElement>();
            if (!first_legend_element_child || !html_element.is_descendant_of(*first_legend_element_child))
                return false;
        }
    }

    return true;
}

void FormAssociatedElement::set_parser_inserted(Badge<HTMLParser>)
{
    m_parser_inserted = true;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#association-of-controls-and-forms:nodes-are-inserted
void FormAssociatedElement::form_node_was_inserted()
{
    // 1. If the form-associated element's parser inserted flag is set, then return.
    if (m_parser_inserted)
        return;

    // 2. Reset the form owner of the form-associated element.
    reset_form_owner();
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#association-of-controls-and-forms:nodes-are-removed
void FormAssociatedElement::form_node_was_removed()
{
    // 1. If the form-associated element has a form owner and the form-associated element and its form owner are no longer in the same tree, then reset the form owner of the form-associated element.
    if (m_form && &form_associated_element_to_html_element().root() != &m_form->root())
        reset_form_owner();
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#reset-the-form-owner
void FormAssociatedElement::reset_form_owner()
{
    auto& html_element = form_associated_element_to_html_element();

    // Although these aren't in the "reset form owner" algorithm, these here as they are triggers for this algorithm:
    // FIXME: When a listed form-associated element's form attribute is set, changed, or removed, then the user agent must reset the form owner of that element.
    // FIXME: When a listed form-associated element has a form attribute and the ID of any of the elements in the tree changes, then the user agent must reset the form owner of that form-associated element.
    // FIXME: When a listed form-associated element has a form attribute and an element with an ID is inserted into or removed from the Document, then the user agent must reset the form owner of that form-associated element.

    // 1. Unset element's parser inserted flag.
    m_parser_inserted = false;

    // 2. If all of the following conditions are true
    //    - element's form owner is not null
    //    - element is not listed or its form content attribute is not present
    //    - element's form owner is its nearest form element ancestor after the change to the ancestor chain
    //    then do nothing, and return.
    if (m_form
        && (!is_listed() || !html_element.has_attribute(HTML::AttributeNames::form))
        && html_element.first_ancestor_of_type<HTMLFormElement>() == m_form.ptr()) {
        return;
    }

    // 3. Set element's form owner to null.
    set_form(nullptr);

    // 4. If element is listed, has a form content attribute, and is connected, then:
    if (is_listed() && html_element.has_attribute(HTML::AttributeNames::form) && html_element.is_connected()) {
        // 1. If the first element in element's tree, in tree order, to have an ID that is identical to element's form content attribute's value, is a form element, then associate the element with that form element.
        auto form_value = html_element.deprecated_attribute(HTML::AttributeNames::form);
        html_element.root().for_each_in_inclusive_subtree_of_type<HTMLFormElement>([this, &form_value](HTMLFormElement& form_element) {
            if (form_element.deprecated_attribute(HTML::AttributeNames::id) == form_value) {
                set_form(&form_element);
                return IterationDecision::Break;
            }

            return IterationDecision::Continue;
        });
    }

    // 5. Otherwise, if element has an ancestor form element, then associate element with the nearest such ancestor form element.
    else {
        auto* form_ancestor = html_element.first_ancestor_of_type<HTMLFormElement>();
        if (form_ancestor)
            set_form(form_ancestor);
    }
}

}
