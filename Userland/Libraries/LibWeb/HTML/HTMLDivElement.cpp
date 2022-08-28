/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLDivElementPrototype.h>
#include <LibWeb/HTML/HTMLDivElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDivElement::HTMLDivElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLDivElementPrototype>("HTMLDivElement"));
}

HTMLDivElement::~HTMLDivElement() = default;
}
