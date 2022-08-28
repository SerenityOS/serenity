/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLHRElementPrototype.h>
#include <LibWeb/HTML/HTMLHRElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLHRElement::HTMLHRElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLHRElementPrototype>("HTMLHRElement"));
}

HTMLHRElement::~HTMLHRElement() = default;
}
