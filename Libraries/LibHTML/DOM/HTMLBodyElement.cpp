/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLBodyElement.h>

namespace Web {

HTMLBodyElement::HTMLBodyElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLBodyElement::~HTMLBodyElement()
{
}

void HTMLBodyElement::apply_presentational_hints(StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("bgcolor")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("text")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("background")) {
            style.set_property(CSS::PropertyID::BackgroundImage, ImageStyleValue::create(document().complete_url(value), const_cast<Document&>(document())));
        }
    });
}

void HTMLBodyElement::parse_attribute(const String& name, const String& value)
{
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
    }
}

}
