/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/Paintable.h>
#include <LibWeb/Painting/ShadowPainting.h>

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

void Box::set_paint_box(OwnPtr<Painting::Paintable> paint_box)
{
    m_paint_box = move(paint_box);
}

// https://www.w3.org/TR/css-display-3/#out-of-flow
bool Box::is_out_of_flow(FormattingContext const& formatting_context) const
{
    // A box is out of flow if either:

    // 1. It is floated (which requires that floating is not inhibited).
    if (!formatting_context.inhibits_floating() && computed_values().float_() != CSS::Float::None)
        return true;

    // 2. It is "absolutely positioned".
    switch (computed_values().position()) {
    case CSS::Position::Absolute:
    case CSS::Position::Fixed:
        return true;
    case CSS::Position::Static:
    case CSS::Position::Relative:
    case CSS::Position::Sticky:
        break;
    }

    return false;
}

HitTestResult Box::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    // FIXME: It would be nice if we could confidently skip over hit testing
    //        parts of the layout tree, but currently we can't just check
    //        m_rect.contains() since inline text rects can't be trusted..
    HitTestResult result { paint_box()->absolute_border_box_rect().contains(position.x(), position.y()) ? this : nullptr };
    for_each_child_in_paint_order([&](auto& child) {
        auto child_result = child.hit_test(position, type);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

void Box::set_needs_display()
{
    if (!is_inline()) {
        browsing_context().set_needs_display(enclosing_int_rect(paint_box()->absolute_rect()));
        return;
    }

    Node::set_needs_display();
}

bool Box::is_body() const
{
    return dom_node() && dom_node() == document().body();
}

OwnPtr<Painting::Paintable> Box::create_paintable() const
{
    return Painting::Paintable::create(*this);
}

}
