/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLDListElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDListElement::HTMLDListElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDListElement::~HTMLDListElement() = default;

void HTMLDListElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDListElementPrototype>(realm, "HTMLDListElement"));
}

}
