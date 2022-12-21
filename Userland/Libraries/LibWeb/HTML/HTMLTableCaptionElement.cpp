/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLTableCaptionElement.h>

namespace Web::HTML {

HTMLTableCaptionElement::HTMLTableCaptionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLTableCaptionElement"));
}

HTMLTableCaptionElement::~HTMLTableCaptionElement() = default;

// https://html.spec.whatwg.org/multipage/rendering.html#tables-2
void HTMLTableCaptionElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    HTMLElement::apply_presentational_hints(style);
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("align"sv)) {
            if (value == "bottom"sv)
                style.set_property(CSS::PropertyID::CaptionSide, CSS::IdentifierStyleValue::create(CSS::ValueID::Bottom));
        }
    });
}

}
