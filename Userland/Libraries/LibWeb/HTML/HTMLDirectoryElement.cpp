/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLDirectoryElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDirectoryElement::HTMLDirectoryElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDirectoryElement::~HTMLDirectoryElement() = default;

void HTMLDirectoryElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDirectoryElementPrototype>(realm, "HTMLDirectoryElement"));
}

}
