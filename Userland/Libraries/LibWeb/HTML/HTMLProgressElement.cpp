/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLProgressElementPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Progress.h>
#include <stdlib.h>

namespace Web::HTML {

HTMLProgressElement::HTMLProgressElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLProgressElementPrototype>("HTMLProgressElement"));
}

HTMLProgressElement::~HTMLProgressElement() = default;

RefPtr<Layout::Node> HTMLProgressElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    RefPtr<Layout::Node> layout_node;
    if (style->appearance().value_or(CSS::Appearance::Auto) == CSS::Appearance::None) {
        layout_node = adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));
        layout_node->set_inline(true);
    } else {
        layout_node = adopt_ref(*new Layout::Progress(document(), *this, move(style)));
    }
    return layout_node;
}

bool HTMLProgressElement::using_system_appearance() const
{
    if (layout_node())
        return is<Layout::Progress>(*layout_node());
    return false;
}

void HTMLProgressElement::progress_position_updated()
{
    if (using_system_appearance())
        layout_node()->set_needs_display();
    else
        document().invalidate_layout();
}

double HTMLProgressElement::value() const
{
    auto value_characters = attribute(HTML::AttributeNames::value).characters();
    if (value_characters == nullptr)
        return 0;

    auto parsed_value = strtod(value_characters, nullptr);
    if (!isfinite(parsed_value) || parsed_value < 0)
        return 0;

    return min(parsed_value, max());
}

void HTMLProgressElement::set_value(double value)
{
    if (value < 0)
        return;

    set_attribute(HTML::AttributeNames::value, String::number(value));
    progress_position_updated();
}

double HTMLProgressElement::max() const
{
    auto max_characters = attribute(HTML::AttributeNames::max).characters();
    if (max_characters == nullptr)
        return 1;

    auto parsed_value = strtod(max_characters, nullptr);
    if (!isfinite(parsed_value) || parsed_value <= 0)
        return 1;

    return parsed_value;
}

void HTMLProgressElement::set_max(double value)
{
    if (value <= 0)
        return;

    set_attribute(HTML::AttributeNames::max, String::number(value));
    progress_position_updated();
}

double HTMLProgressElement::position() const
{
    if (!is_determinate())
        return -1;

    return value() / max();
}

}
