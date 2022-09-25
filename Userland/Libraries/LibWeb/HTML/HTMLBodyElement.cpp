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
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLBodyElement"));
}

HTMLBodyElement::~HTMLBodyElement() = default;

void HTMLBodyElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("bgcolor"sv)) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, CSS::ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("text"sv)) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, CSS::ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("background"sv)) {
            VERIFY(m_background_style_value);
            style.set_property(CSS::PropertyID::BackgroundImage, *m_background_style_value);
        }
    });
}

void HTMLBodyElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name.equals_ignoring_case("link"sv)) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_link_color(color.value());
    } else if (name.equals_ignoring_case("alink"sv)) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_active_link_color(color.value());
    } else if (name.equals_ignoring_case("vlink"sv)) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_visited_link_color(color.value());
    } else if (name.equals_ignoring_case("background"sv)) {
        m_background_style_value = CSS::ImageStyleValue::create(document().parse_url(value));
    }

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                     \
    if (name == HTML::AttributeNames::attribute_name) {             \
        element_event_handler_attribute_changed(event_name, value); \
    }
    ENUMERATE_WINDOW_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE
}

DOM::EventTarget& HTMLBodyElement::global_event_handlers_to_event_target(FlyString const& event_name)
{
    // NOTE: This is a little weird, but IIUC document.body.onload actually refers to window.onload
    // NOTE: document.body can return either a HTMLBodyElement or HTMLFrameSetElement, so both these elements must support this mapping.
    if (DOM::is_window_reflecting_body_element_event_handler(event_name))
        return document().window();

    return *this;
}

DOM::EventTarget& HTMLBodyElement::window_event_handlers_to_event_target()
{
    // All WindowEventHandlers on HTMLFrameSetElement (e.g. document.body.onrejectionhandled) are mapped to window.on{event}.
    // NOTE: document.body can return either a HTMLBodyElement or HTMLFrameSetElement, so both these elements must support this mapping.
    return document().window();
}

}
