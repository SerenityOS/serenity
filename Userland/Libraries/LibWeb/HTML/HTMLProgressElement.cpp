/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/Layout/Progress.h>
#include <stdlib.h>

namespace Web::HTML {

HTMLProgressElement::HTMLProgressElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLProgressElement::~HTMLProgressElement()
{
}

RefPtr<Layout::Node> HTMLProgressElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::Progress(document(), *this, move(style)));
}

double HTMLProgressElement::value() const
{
    auto parsed_value = strtod(attribute(HTML::AttributeNames::value).characters(), nullptr);
    if (!isfinite(parsed_value) || parsed_value < 0)
        return 0;

    return min(parsed_value, max());
}

void HTMLProgressElement::set_value(double value)
{
    if (value < 0)
        return;

    set_attribute(HTML::AttributeNames::value, String::number(value));

    if (layout_node())
        layout_node()->set_needs_display();
}

double HTMLProgressElement::max() const
{
    auto parsed_value = strtod(attribute(HTML::AttributeNames::max).characters(), nullptr);
    if (!isfinite(parsed_value) || parsed_value <= 0)
        return 1;

    return parsed_value;
}

void HTMLProgressElement::set_max(double value)
{
    if (value <= 0)
        return;

    set_attribute(HTML::AttributeNames::max, String::number(value));

    if (layout_node())
        layout_node()->set_needs_display();
}

double HTMLProgressElement::position() const
{
    if (!is_determinate())
        return -1;

    return value() / max();
}

}
