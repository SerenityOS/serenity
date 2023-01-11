/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLOutputElement.h>

namespace Web::HTML {

HTMLOutputElement::HTMLOutputElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLOutputElement::~HTMLOutputElement() = default;

void HTMLOutputElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLOutputElementPrototype>(realm, "HTMLOutputElement"));
}

// https://html.spec.whatwg.org/multipage/form-elements.html#the-output-element:concept-form-reset-control
void HTMLOutputElement::reset_algorithm()
{
    // 1. FIXME: String replace all with this element's default value within this element.

    // 2. FIXME: Set this element's default value override to null.
}

}
