/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Layout {

BlockContainer::BlockContainer(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
    : Box(document, node, move(style))
{
}

BlockContainer::BlockContainer(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
    : Box(document, node, move(computed_values))
{
}

BlockContainer::~BlockContainer()
{
}

HitTestResult BlockContainer::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    if (!children_are_inline())
        return Box::hit_test(position, type);

    HitTestResult last_good_candidate;
    for (auto& line_box : paint_box()->line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (is<Box>(fragment.layout_node()) && verify_cast<Box>(fragment.layout_node()).paint_box()->stacking_context())
                continue;
            if (enclosing_int_rect(fragment.absolute_rect()).contains(position)) {
                if (is<BlockContainer>(fragment.layout_node()))
                    return verify_cast<BlockContainer>(fragment.layout_node()).hit_test(position, type);
                return { fragment.layout_node(), fragment.text_index_at(position.x()) };
            }
            if (fragment.absolute_rect().top() <= position.y())
                last_good_candidate = { fragment.layout_node(), fragment.text_index_at(position.x()) };
        }
    }

    if (type == HitTestType::TextCursor && last_good_candidate.layout_node)
        return last_good_candidate;
    return { paint_box()->absolute_border_box_rect().contains(position.x(), position.y()) ? this : nullptr };
}

bool BlockContainer::is_scrollable() const
{
    // FIXME: Support horizontal scroll as well (overflow-x)
    return computed_values().overflow_y() == CSS::Overflow::Scroll;
}

void BlockContainer::set_scroll_offset(const Gfx::FloatPoint& offset)
{
    // FIXME: If there is horizontal and vertical scroll ignore only part of the new offset
    if (offset.y() < 0 || m_scroll_offset == offset)
        return;
    m_scroll_offset = offset;
    set_needs_display();
}

bool BlockContainer::handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned int, unsigned int, int wheel_delta_x, int wheel_delta_y)
{
    if (!is_scrollable())
        return false;
    auto new_offset = m_scroll_offset;
    new_offset.translate_by(wheel_delta_x, wheel_delta_y);
    set_scroll_offset(new_offset);

    return true;
}

Painting::PaintableWithLines const* BlockContainer::paint_box() const
{
    return static_cast<Painting::PaintableWithLines const*>(Box::paint_box());
}

OwnPtr<Painting::Paintable> BlockContainer::create_paintable() const
{
    return Painting::PaintableWithLines::create(*this);
}

}
