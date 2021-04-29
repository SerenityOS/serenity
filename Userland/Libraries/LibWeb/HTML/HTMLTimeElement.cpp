/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLTimeElement.h>

namespace Web::HTML {

HTMLTimeElement::HTMLTimeElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTimeElement::~HTMLTimeElement()
{
}

}
