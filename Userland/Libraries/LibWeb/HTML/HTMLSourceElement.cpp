/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLSourceElement.h>

namespace Web::HTML {

HTMLSourceElement::HTMLSourceElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLSourceElement::~HTMLSourceElement() = default;

void HTMLSourceElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLSourceElementPrototype>(realm, "HTMLSourceElement"));
}

}
