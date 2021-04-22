/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/HTML/HTMLTableElement.h>

namespace Web::HTML {

HTMLTableElement::HTMLTableElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableElement::~HTMLTableElement()
{
}

void HTMLTableElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_html_length(document(), value))
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
            return;
        }
        if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_html_length(document(), value))
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
            return;
        }
        if (name == HTML::AttributeNames::bgcolor) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, CSS::ColorStyleValue::create(color.value()));
            return;
        }
    });
}

}
