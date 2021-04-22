/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLUListElement.h>

namespace Web::HTML {

HTMLUListElement::HTMLUListElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLUListElement::~HTMLUListElement()
{
}

}
