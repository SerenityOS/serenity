/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLMeterElementPrototype.h>
#include <LibWeb/HTML/HTMLMeterElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLMeterElement::HTMLMeterElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLMeterElementPrototype>("HTMLMeterElement"));
}

HTMLMeterElement::~HTMLMeterElement() = default;

}
