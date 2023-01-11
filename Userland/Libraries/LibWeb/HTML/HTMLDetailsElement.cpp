/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLDetailsElement.h>

namespace Web::HTML {

HTMLDetailsElement::HTMLDetailsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDetailsElement::~HTMLDetailsElement() = default;

void HTMLDetailsElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDetailsElementPrototype>(realm, "HTMLDetailsElement"));
}

}
