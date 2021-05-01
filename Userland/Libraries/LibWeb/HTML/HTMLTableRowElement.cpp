/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLTableRowElement.h>

namespace Web::HTML {

HTMLTableRowElement::HTMLTableRowElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableRowElement::~HTMLTableRowElement()
{
}

}
