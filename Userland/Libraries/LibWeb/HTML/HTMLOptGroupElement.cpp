/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLOptGroupElementPrototype.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLOptGroupElement::HTMLOptGroupElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLOptGroupElementPrototype>("HTMLOptGroupElement"));
}

HTMLOptGroupElement::~HTMLOptGroupElement() = default;

}
