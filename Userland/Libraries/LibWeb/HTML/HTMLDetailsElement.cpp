/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLDetailsElementPrototype.h>
#include <LibWeb/HTML/HTMLDetailsElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDetailsElement::HTMLDetailsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLDetailsElementPrototype>("HTMLDetailsElement"));
}

HTMLDetailsElement::~HTMLDetailsElement() = default;
}
