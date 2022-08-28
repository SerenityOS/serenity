/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLDataListElementPrototype.h>
#include <LibWeb/HTML/HTMLDataListElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDataListElement::HTMLDataListElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLDataListElementPrototype>("HTMLDataListElement"));
}

HTMLDataListElement::~HTMLDataListElement() = default;
}
