/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>

namespace Web::HTML {

HTMLTextAreaElement::HTMLTextAreaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTextAreaElement::~HTMLTextAreaElement() = default;

void HTMLTextAreaElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLTextAreaElementPrototype>(realm, "HTMLTextAreaElement"));
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLTextAreaElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

// https://html.spec.whatwg.org/multipage/form-elements.html#the-textarea-element:concept-form-reset-control
void HTMLTextAreaElement::reset_algorithm()
{
    // FIXME: The reset algorithm for textarea elements is to set the dirty value flag back to false, and set the raw value of element to its child text content.
}

}
