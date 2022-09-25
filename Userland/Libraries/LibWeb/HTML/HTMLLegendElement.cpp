/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLLegendElementPrototype.h>
#include <LibWeb/HTML/HTMLLegendElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLLegendElement::HTMLLegendElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLLegendElement"));
}

HTMLLegendElement::~HTMLLegendElement() = default;
}
