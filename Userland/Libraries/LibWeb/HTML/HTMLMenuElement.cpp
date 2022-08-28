/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLMenuElementPrototype.h>
#include <LibWeb/HTML/HTMLMenuElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLMenuElement::HTMLMenuElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLMenuElementPrototype>("HTMLMenuElement"));
}

HTMLMenuElement::~HTMLMenuElement() = default;

}
