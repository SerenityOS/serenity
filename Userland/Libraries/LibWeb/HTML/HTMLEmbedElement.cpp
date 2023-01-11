/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>

namespace Web::HTML {

HTMLEmbedElement::HTMLEmbedElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLEmbedElement::~HTMLEmbedElement() = default;

void HTMLEmbedElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLEmbedElementPrototype>(realm, "HTMLEmbedElement"));
}

}
