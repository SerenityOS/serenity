/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLHtmlElement.h>

namespace Web::HTML {

HTMLHtmlElement::HTMLHtmlElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLHtmlElement::~HTMLHtmlElement()
{
}

bool HTMLHtmlElement::should_use_body_background_properties() const
{
    auto background_color = layout_node()->computed_values().background_color();
    const auto* background_image = layout_node()->background_image();

    return (background_color == Color::Transparent) && !background_image;
}

}
