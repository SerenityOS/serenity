/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLUnknownElementPrototype.h>
#include <LibWeb/HTML/HTMLUnknownElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLUnknownElement::HTMLUnknownElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLUnknownElementPrototype>("HTMLUnknownElement"));
}

HTMLUnknownElement::~HTMLUnknownElement() = default;

}
