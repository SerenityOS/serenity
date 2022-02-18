/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLOutputElement.h>

namespace Web::HTML {

HTMLOutputElement::HTMLOutputElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
{
}

HTMLOutputElement::~HTMLOutputElement()
{
}

}
