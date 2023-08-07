/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLSpanElement.h>

namespace Web::HTML {

HTMLSpanElement::HTMLSpanElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLSpanElement::~HTMLSpanElement() = default;

void HTMLSpanElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLSpanElementPrototype>(realm, "HTMLSpanElement"));
}

}
