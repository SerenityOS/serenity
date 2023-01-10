/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLUnknownElement.h>

namespace Web::HTML {

HTMLUnknownElement::HTMLUnknownElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLUnknownElement::~HTMLUnknownElement() = default;

void HTMLUnknownElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLUnknownElementPrototype>(realm, "HTMLUnknownElement"));
}

}
