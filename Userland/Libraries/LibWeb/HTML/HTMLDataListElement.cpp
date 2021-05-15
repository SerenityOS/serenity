/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLDataListElement.h>

namespace Web::HTML {

HTMLDataListElement::HTMLDataListElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDataListElement::~HTMLDataListElement()
{
}

}
