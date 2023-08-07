/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLLIElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLLIElement::HTMLLIElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLIElement::~HTMLLIElement() = default;

void HTMLLIElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLLIElementPrototype>(realm, "HTMLLIElement"));
}

}
