/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLHeadElementPrototype.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLHeadElement::HTMLHeadElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLHeadElementPrototype>("HTMLHeadElement"));
}

HTMLHeadElement::~HTMLHeadElement() = default;
}
