/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLTableCaptionElement.h>

namespace Web::HTML {

HTMLTableCaptionElement::HTMLTableCaptionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLTableCaptionElement"));
}

HTMLTableCaptionElement::~HTMLTableCaptionElement() = default;

}
