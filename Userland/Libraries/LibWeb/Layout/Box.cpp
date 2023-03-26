/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Layout {

Box::Box(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
    : NodeWithStyleAndBoxModelMetrics(document, node, move(style))
{
}

Box::Box(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
    : NodeWithStyleAndBoxModelMetrics(document, node, move(computed_values))
{
}

Box::~Box()
{
}

// https://www.w3.org/TR/css-overflow-3/#overflow-control
static bool overflow_value_makes_box_a_scroll_container(CSS::Overflow overflow)
{
    switch (overflow) {
    case CSS::Overflow::Clip:
    case CSS::Overflow::Visible:
        return false;
    case CSS::Overflow::Auto:
    case CSS::Overflow::Hidden:
    case CSS::Overflow::Scroll:
        return true;
    }
    VERIFY_NOT_REACHED();
}

// https://www.w3.org/TR/css-overflow-3/#scroll-container
bool Box::is_scroll_container() const
{
    return overflow_value_makes_box_a_scroll_container(computed_values().overflow_x())
        || overflow_value_makes_box_a_scroll_container(computed_values().overflow_y());
}

bool Box::is_scrollable() const
{
    // FIXME: Support horizontal scroll as well (overflow-x)
    return computed_values().overflow_y() == CSS::Overflow::Scroll;
}

void Box::set_scroll_offset(CSSPixelPoint offset)
{
    // FIXME: If there is horizontal and vertical scroll ignore only part of the new offset
    if (offset.y() < 0 || m_scroll_offset == offset)
        return;
    m_scroll_offset = offset;
    set_needs_display();
}

void Box::set_needs_display()
{
    if (paint_box())
        browsing_context().set_needs_display(paint_box()->absolute_rect());
}

bool Box::is_body() const
{
    return dom_node() && dom_node() == document().body();
}

JS::GCPtr<Painting::Paintable> Box::create_paintable() const
{
    return Painting::PaintableBox::create(*this);
}

Painting::PaintableBox const* Box::paint_box() const
{
    return static_cast<Painting::PaintableBox const*>(Node::paintable());
}

}
