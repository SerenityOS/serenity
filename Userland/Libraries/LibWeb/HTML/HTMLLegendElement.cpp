/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLLegendElement.h>

namespace Web::HTML {

HTMLLegendElement::HTMLLegendElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLegendElement::~HTMLLegendElement() = default;

void HTMLLegendElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLLegendElementPrototype>(realm, "HTMLLegendElement"));
}

}
