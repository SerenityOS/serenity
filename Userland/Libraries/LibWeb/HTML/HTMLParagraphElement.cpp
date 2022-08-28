/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLParagraphElementPrototype.h>
#include <LibWeb/HTML/HTMLParagraphElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLParagraphElement::HTMLParagraphElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLParagraphElementPrototype>("HTMLParagraphElement"));
}

HTMLParagraphElement::~HTMLParagraphElement() = default;

}
