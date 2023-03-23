/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/HTML/HTMLFontElement.h>

namespace Web::HTML {

HTMLFontElement::HTMLFontElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFontElement::~HTMLFontElement() = default;

JS::ThrowCompletionOr<void> HTMLFontElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLFontElementPrototype>(realm, "HTMLFontElement"));

    return {};
}

void HTMLFontElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_ascii_case("color"sv)) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, CSS::ColorStyleValue::create(color.value()));
        }
    });
}

}
