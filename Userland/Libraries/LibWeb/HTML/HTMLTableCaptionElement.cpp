/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLTableCaptionElementPrototype.h>
#include <LibWeb/HTML/HTMLTableCaptionElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLTableCaptionElement::HTMLTableCaptionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLTableCaptionElementPrototype>("HTMLTableCaptionElement"));
}

HTMLTableCaptionElement::~HTMLTableCaptionElement() = default;

}
