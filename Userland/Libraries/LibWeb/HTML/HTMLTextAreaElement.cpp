/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLTextAreaElement.h>

namespace Web::HTML {

HTMLTextAreaElement::HTMLTextAreaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
{
}

HTMLTextAreaElement::~HTMLTextAreaElement()
{
}

}
