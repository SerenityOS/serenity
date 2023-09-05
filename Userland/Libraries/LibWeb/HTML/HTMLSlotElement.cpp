/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLSlotElement.h>

namespace Web::HTML {

HTMLSlotElement::HTMLSlotElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLSlotElement::~HTMLSlotElement() = default;

void HTMLSlotElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLSlotElementPrototype>(realm, "HTMLSlotElement"));
}

void HTMLSlotElement::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    Slot::visit_edges(visitor);
}

}
