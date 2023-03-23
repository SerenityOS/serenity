/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/HTML/HTMLMarqueeElement.h>

namespace Web::HTML {

HTMLMarqueeElement::HTMLMarqueeElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMarqueeElement::~HTMLMarqueeElement() = default;

JS::ThrowCompletionOr<void> HTMLMarqueeElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLMarqueeElementPrototype>(realm, "HTMLMarqueeElement"));

    return {};
}

void HTMLMarqueeElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    HTMLElement::apply_presentational_hints(style);
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::bgcolor) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, CSS::ColorStyleValue::create(color.value()));
        }
    });
}

}
