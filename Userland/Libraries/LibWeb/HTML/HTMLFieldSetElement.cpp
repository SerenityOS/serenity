/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/HTMLLegendElement.h>

namespace Web::HTML {

HTMLFieldSetElement::HTMLFieldSetElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFieldSetElement::~HTMLFieldSetElement() = default;

void HTMLFieldSetElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLFieldSetElementPrototype>(realm, "HTMLFieldSetElement"));
}

// https://html.spec.whatwg.org/multipage/form-elements.html#concept-fieldset-disabled
bool HTMLFieldSetElement::is_disabled() const
{
    // A fieldset element is a disabled fieldset if it matches any of the following conditions:
    // - Its disabled attribute is specified
    if (has_attribute(AttributeNames::disabled))
        return true;

    // - It is a descendant of another fieldset element whose disabled attribute is specified, and is not a descendant of that fieldset element's first legend element child, if any.
    for (auto* fieldset_ancestor = first_ancestor_of_type<HTMLFieldSetElement>(); fieldset_ancestor; fieldset_ancestor = fieldset_ancestor->first_ancestor_of_type<HTMLFieldSetElement>()) {
        if (fieldset_ancestor->has_attribute(HTML::AttributeNames::disabled)) {
            auto* first_legend_element_child = fieldset_ancestor->first_child_of_type<HTMLLegendElement>();
            if (!first_legend_element_child || !is_descendant_of(*first_legend_element_child))
                return true;
        }
    }

    return false;
}

}
