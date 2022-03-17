/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLFrameSetElement.h>

namespace Web::HTML {

HTMLFrameSetElement::HTMLFrameSetElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFrameSetElement::~HTMLFrameSetElement() = default;
}
