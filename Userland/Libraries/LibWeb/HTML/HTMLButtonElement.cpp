/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLButtonElement.h>

namespace Web::HTML {

HTMLButtonElement::HTMLButtonElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLButtonElement::~HTMLButtonElement()
{
}

}
