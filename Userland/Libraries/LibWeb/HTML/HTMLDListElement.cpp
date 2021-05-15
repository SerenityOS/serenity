/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLDListElement.h>

namespace Web::HTML {

HTMLDListElement::HTMLDListElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDListElement::~HTMLDListElement()
{
}

}
