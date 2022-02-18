/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLFrameElement.h>

namespace Web::HTML {

HTMLFrameElement::HTMLFrameElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFrameElement::~HTMLFrameElement()
{
}

}
