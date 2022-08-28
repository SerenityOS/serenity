/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLHeadingElementPrototype.h>
#include <LibWeb/HTML/HTMLHeadingElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLHeadingElement::HTMLHeadingElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLHeadingElementPrototype>("HTMLHeadingElement"));
}

HTMLHeadingElement::~HTMLHeadingElement() = default;
}
