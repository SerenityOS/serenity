/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>

namespace Web::HTML {

HTMLSelectElement::HTMLSelectElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLSelectElement::~HTMLSelectElement()
{
}

void HTMLSelectElement::inserted()
{
    HTMLElement::inserted();
    set_form(first_ancestor_of_type<HTMLFormElement>());
}

void HTMLSelectElement::removed_from(DOM::Node* old_parent)
{
    HTMLElement::removed_from(old_parent);
    set_form(nullptr);
}

}
