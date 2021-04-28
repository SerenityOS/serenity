/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLTableSectionElement.h>

namespace Web::HTML {

HTMLTableSectionElement::HTMLTableSectionElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableSectionElement::~HTMLTableSectionElement()
{
}

}
