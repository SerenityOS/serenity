/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLFieldSetElement.h>

namespace Web::HTML {

HTMLFieldSetElement::HTMLFieldSetElement(DOM::Document& document, QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
{
}

HTMLFieldSetElement::~HTMLFieldSetElement()
{
}

}
