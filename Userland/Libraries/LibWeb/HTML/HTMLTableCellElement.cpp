/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>

namespace Web::HTML {

HTMLTableCellElement::HTMLTableCellElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableCellElement::~HTMLTableCellElement()
{
}

void HTMLTableCellElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::bgcolor) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, CSS::ColorStyleValue::create(color.value()));
            return;
        }
        if (name == HTML::AttributeNames::align) {
            if (value.equals_ignoring_case("center") || value.equals_ignoring_case("middle"))
                style.set_property(CSS::PropertyID::TextAlign, StringView("-libweb-center"));
            else
                style.set_property(CSS::PropertyID::TextAlign, value.view());
            return;
        }
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_html_length(document(), value))
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
            return;
        }
    });
}

}
