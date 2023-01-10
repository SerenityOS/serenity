/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLMenuElement.h>

namespace Web::HTML {

HTMLMenuElement::HTMLMenuElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMenuElement::~HTMLMenuElement() = default;

void HTMLMenuElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLMenuElementPrototype>(realm, "HTMLMenuElement"));
}

}
