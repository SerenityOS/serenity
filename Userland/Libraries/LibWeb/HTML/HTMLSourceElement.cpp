/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLSourceElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLSourceElement::HTMLSourceElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("HTMLSourceElement"));
}

HTMLSourceElement::~HTMLSourceElement() = default;

}
