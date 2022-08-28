/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLTableColElementPrototype.h>
#include <LibWeb/HTML/HTMLTableColElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLTableColElement::HTMLTableColElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLTableColElementPrototype>("HTMLTableColElement"));
}

HTMLTableColElement::~HTMLTableColElement() = default;

}
