/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLMapElement.h>

namespace Web::HTML {

HTMLMapElement::HTMLMapElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMapElement::~HTMLMapElement()
{
}

}
