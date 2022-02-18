/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLBaseElement.h>

namespace Web::HTML {

HTMLBaseElement::HTMLBaseElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLBaseElement::~HTMLBaseElement()
{
}

}
