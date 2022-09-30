/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLMeterElement.h>

namespace Web::HTML {

HTMLMeterElement::HTMLMeterElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLMeterElement"));
}

HTMLMeterElement::~HTMLMeterElement() = default;

}
