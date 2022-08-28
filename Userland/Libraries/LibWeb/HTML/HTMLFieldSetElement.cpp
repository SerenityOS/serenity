/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLFieldSetElementPrototype.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLFieldSetElement::HTMLFieldSetElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLFieldSetElementPrototype>("HTMLFieldSetElement"));
}

HTMLFieldSetElement::~HTMLFieldSetElement() = default;
}
