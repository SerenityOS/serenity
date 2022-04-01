/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLBodyElement::HTMLBodyElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLBodyElement::~HTMLBodyElement() = default;

void HTMLBodyElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("bgcolor")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, CSS::ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("text")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, CSS::ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("background")) {
            VERIFY(m_background_style_value);
            style.set_property(CSS::PropertyID::BackgroundImage, *m_background_style_value);
        }
    });
}

void HTMLBodyElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name.equals_ignoring_case("link")) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_link_color(color.value());
    } else if (name.equals_ignoring_case("alink")) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_active_link_color(color.value());
    } else if (name.equals_ignoring_case("vlink")) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_visited_link_color(color.value());
    } else if (name.equals_ignoring_case("background")) {
        m_background_style_value = CSS::ImageStyleValue::create(document().parse_url(value));
    }
}

DOM::EventTarget& HTMLBodyElement::global_event_handlers_to_event_target()
{
    // NOTE: This is a little weird, but IIUC document.body.onload actually refers to window.onload
    return document().window();
}

}
