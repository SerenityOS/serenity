/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLProgressElement.h>

namespace Web::HTML {

HTMLProgressElement::HTMLProgressElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLProgressElement::~HTMLProgressElement()
{
}

}
