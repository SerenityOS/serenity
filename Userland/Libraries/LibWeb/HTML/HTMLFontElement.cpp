/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/HTML/HTMLFontElement.h>

namespace Web::HTML {

HTMLFontElement::HTMLFontElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFontElement::~HTMLFontElement()
{
}

void HTMLFontElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("color")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, CSS::ColorStyleValue::create(color.value()));
        }
    });
}

}
