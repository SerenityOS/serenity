/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLOListElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLOListElement::HTMLOListElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("HTMLOListElement"));
}

HTMLOListElement::~HTMLOListElement() = default;

}
