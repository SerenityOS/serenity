/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

HTMLMediaElement::HTMLMediaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMediaElement::~HTMLMediaElement()
{
}

}
