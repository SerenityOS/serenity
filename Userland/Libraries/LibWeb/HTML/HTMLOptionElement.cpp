/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLOptionElement.h>

namespace Web::HTML {

HTMLOptionElement::HTMLOptionElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLOptionElement::~HTMLOptionElement()
{
}

}
