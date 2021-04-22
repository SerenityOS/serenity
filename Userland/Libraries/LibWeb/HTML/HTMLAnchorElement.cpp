/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLAnchorElement.h>

namespace Web::HTML {

HTMLAnchorElement::HTMLAnchorElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLAnchorElement::~HTMLAnchorElement()
{
}

}
