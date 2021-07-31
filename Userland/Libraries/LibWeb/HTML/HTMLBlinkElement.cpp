/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/HTML/HTMLBlinkElement.h>

namespace Web::HTML {

HTMLBlinkElement::HTMLBlinkElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_timer(Core::Timer::construct())
{
    m_timer->set_interval(500);
    m_timer->on_timeout = [this] { blink(); };
    m_timer->start();
}

HTMLBlinkElement::~HTMLBlinkElement()
{
}

void HTMLBlinkElement::blink()
{
    if (!layout_node())
        return;

    layout_node()->set_visible(!layout_node()->is_visible());
    layout_node()->set_needs_display();
}

}
