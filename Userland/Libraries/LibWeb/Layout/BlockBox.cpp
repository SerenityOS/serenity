/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/Painter.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

BlockBox::BlockBox(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
    : Box(document, node, move(style))
{
}

BlockBox::BlockBox(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
    : Box(document, node, move(computed_values))
{
}

BlockBox::~BlockBox()
{
}

bool BlockBox::should_clip_overflow() const
{
    return computed_values().overflow_x() != CSS::Overflow::Visible && computed_values().overflow_y() != CSS::Overflow::Visible;
}

void BlockBox::paint(PaintContext& context, PaintPhase phase)
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

HitTestResult BlockBox::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    if (!children_are_inline())
        return Box::hit_test(position, type);

    HitTestResult last_good_candidate;
    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (is<Box>(fragment.layout_node()) && downcast<Box>(fragment.layout_node()).stacking_context())
                continue;
            if (enclosing_int_rect(fragment.absolute_rect()).contains(position)) {
                if (is<BlockBox>(fragment.layout_node()))
                    return downcast<BlockBox>(fragment.layout_node()).hit_test(position, type);
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

void BlockBox::split_into_lines(InlineFormattingContext& context, LayoutMode layout_mode)
{
    auto& containing_block = context.containing_block();
    auto* line_box = &containing_block.ensure_last_line_box();

    context.dimension_box_on_line(*this, layout_mode);

    float available_width = context.available_width_at_line(containing_block.line_boxes().size() - 1);

    if (layout_mode == LayoutMode::AllPossibleLineBreaks && line_box->width() > 0) {
        line_box = &containing_block.add_line_box();
    } else if (layout_mode == LayoutMode::Default && line_box->width() > 0 && line_box->width() + border_box_width() > available_width) {
        line_box = &containing_block.add_line_box();
    }
    line_box->add_fragment(*this, 0, 0, border_box_width(), height());
}

void BlockBox::set_scroll_offset(const Gfx::FloatPoint& offset)
{
    if (m_scroll_offset == offset)
        return;
    m_scroll_offset = offset;
    set_needs_display();
}

void BlockBox::handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned int, unsigned int, int wheel_delta)
{
    auto new_offset = m_scroll_offset;
    new_offset.move_by(0, wheel_delta);
    set_scroll_offset(new_offset);
}

}
