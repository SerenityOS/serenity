/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLDataElementPrototype.h>
#include <LibWeb/HTML/HTMLDataElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDataElement::HTMLDataElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLDataElementPrototype>("HTMLDataElement"));
}

HTMLDataElement::~HTMLDataElement() = default;
}
