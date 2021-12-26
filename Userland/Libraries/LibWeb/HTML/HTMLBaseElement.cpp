/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLBaseElement.h>

namespace Web::HTML {

HTMLBaseElement::HTMLBaseElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLBaseElement::~HTMLBaseElement()
{
}

}
