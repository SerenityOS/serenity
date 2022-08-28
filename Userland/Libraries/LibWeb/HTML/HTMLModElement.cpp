/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLModElementPrototype.h>
#include <LibWeb/HTML/HTMLModElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLModElement::HTMLModElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLModElementPrototype>("HTMLModElement"));
}

HTMLModElement::~HTMLModElement() = default;

}
