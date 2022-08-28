/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLOutputElementPrototype.h>
#include <LibWeb/HTML/HTMLOutputElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLOutputElement::HTMLOutputElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLOutputElementPrototype>("HTMLOutputElement"));
}

HTMLOutputElement::~HTMLOutputElement() = default;

}
