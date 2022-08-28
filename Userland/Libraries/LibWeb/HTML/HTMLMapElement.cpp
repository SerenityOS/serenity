/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLMapElementPrototype.h>
#include <LibWeb/HTML/HTMLMapElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLMapElement::HTMLMapElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLMapElementPrototype>("HTMLMapElement"));
}

HTMLMapElement::~HTMLMapElement() = default;
}
