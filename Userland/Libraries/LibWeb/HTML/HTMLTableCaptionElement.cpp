/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLTableCaptionElement.h>

namespace Web::HTML {

HTMLTableCaptionElement::HTMLTableCaptionElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableCaptionElement::~HTMLTableCaptionElement()
{
}

}
