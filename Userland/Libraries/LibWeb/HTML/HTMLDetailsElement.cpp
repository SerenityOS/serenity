/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLDetailsElement.h>

namespace Web::HTML {

HTMLDetailsElement::HTMLDetailsElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDetailsElement::~HTMLDetailsElement()
{
}

}
