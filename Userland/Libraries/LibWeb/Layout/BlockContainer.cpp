/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

bool BlockContainer::should_clip_overflow() const
{
    return computed_values().overflow_x() != CSS::Overflow::Visible && computed_values().overflow_y() != CSS::Overflow::Visible;
}

void BlockContainer::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    Box::paint(context, phase);

    if (!children_are_inline())
        return;

    if (should_clip_overflow()) {
        context.painter().save();
        // FIXME: Handle overflow-x and overflow-y being different values.
        context.painter().add_clip_rect(enclosing_int_rect(padded_rect()));
        context.painter().translate(-m_scroll_offset.to_type<int>());
    }

    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (context.should_show_line_box_borders())
                context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), Color::Green);
            fragment.paint(context, phase);
        }
    }

    if (should_clip_overflow()) {
        context.painter().restore();
    }

    // FIXME: Merge this loop with the above somehow..
    if (phase == PaintPhase::FocusOutline) {
        for (auto& line_box : m_line_boxes) {
            for (auto& fragment : line_box.fragments()) {
                auto* node = fragment.layout_node().dom_node();
                if (!node)
                    continue;
                auto* parent = node->parent_element();
                if (!parent)
                    continue;
                if (parent->is_focused())
                    context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), context.palette().focus_outline());
            }
        }
    }
}

HitTestResult BlockContainer::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    if (!children_are_inline())
        return Box::hit_test(position, type);

    HitTestResult last_good_candidate;
    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (is<Box>(fragment.layout_node()) && verify_cast<Box>(fragment.layout_node()).stacking_context())
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
    return { absolute_rect().contains(position.x(), position.y()) ? this : nullptr };
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

LineBox& BlockContainer::ensure_last_line_box()
{
    if (m_line_boxes.is_empty())
        return add_line_box();
    return m_line_boxes.last();
}

LineBox& BlockContainer::add_line_box()
{
    m_line_boxes.append(LineBox());
    return m_line_boxes.last();
}

}
