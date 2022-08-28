/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLTextAreaElementPrototype.h>
#include <LibWeb/HTML/HTMLTimeElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLTimeElement::HTMLTimeElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLTextAreaElementPrototype>("HTMLTextAreaElement"));
}

HTMLTimeElement::~HTMLTimeElement() = default;

}
