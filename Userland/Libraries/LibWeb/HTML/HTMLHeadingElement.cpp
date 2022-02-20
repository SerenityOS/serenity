/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLHeadingElement.h>

namespace Web::HTML {

HTMLHeadingElement::HTMLHeadingElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLHeadingElement::~HTMLHeadingElement()
{
}

}
