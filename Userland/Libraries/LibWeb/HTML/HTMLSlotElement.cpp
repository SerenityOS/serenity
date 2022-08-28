/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLSlotElementPrototype.h>
#include <LibWeb/HTML/HTMLSlotElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLSlotElement::HTMLSlotElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLSlotElementPrototype>("HTMLSlotElement"));
}

HTMLSlotElement::~HTMLSlotElement() = default;

}
