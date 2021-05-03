/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLSourceElement.h>

namespace Web::HTML {

HTMLSourceElement::HTMLSourceElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLSourceElement::~HTMLSourceElement()
{
}

}
