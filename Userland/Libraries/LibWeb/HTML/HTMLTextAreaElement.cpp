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
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLTextAreaElement"));
}

HTMLTextAreaElement::~HTMLTextAreaElement() = default;

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLTextAreaElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

}
