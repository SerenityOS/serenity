/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>

namespace Web::HTML {

HTMLOptGroupElement::HTMLOptGroupElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLOptGroupElement"));
}

HTMLOptGroupElement::~HTMLOptGroupElement() = default;

}
