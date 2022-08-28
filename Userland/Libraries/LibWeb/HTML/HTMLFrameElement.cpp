/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLFrameElementPrototype.h>
#include <LibWeb/HTML/HTMLFrameElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLFrameElement::HTMLFrameElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLFrameElementPrototype>("HTMLFrameElement"));
}

HTMLFrameElement::~HTMLFrameElement() = default;
}
