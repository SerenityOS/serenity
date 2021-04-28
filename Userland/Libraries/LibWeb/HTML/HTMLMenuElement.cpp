/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLMenuElement.h>

namespace Web::HTML {

HTMLMenuElement::HTMLMenuElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMenuElement::~HTMLMenuElement()
{
}

}
