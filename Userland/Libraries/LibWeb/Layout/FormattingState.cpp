/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingState.h>

namespace Web::Layout {

void FormattingState::commit()
{
    for (auto& it : nodes) {
        auto& node = const_cast<Layout::NodeWithStyleAndBoxModelMetrics&>(*it.key);
        auto& node_state = *it.value;

        // Transfer box model metrics.
        node.box_model().offset = { node_state.offset_top, node_state.offset_right, node_state.offset_bottom, node_state.offset_left };
        node.box_model().padding = { node_state.padding_top, node_state.padding_right, node_state.padding_bottom, node_state.padding_left };
        node.box_model().border = { node_state.border_top, node_state.border_right, node_state.border_bottom, node_state.border_left };
        node.box_model().margin = { node_state.margin_top, node_state.margin_right, node_state.margin_bottom, node_state.margin_left };

        // For boxes, transfer relative offset, size, and overflow data.
        if (is<Layout::Box>(node)) {
            auto& box = static_cast<Layout::Box&>(node);
            box.set_offset(node_state.offset);
            box.set_content_size(node_state.content_width, node_state.content_height);
            box.set_overflow_data(move(node_state.overflow_data));
        }

        // For block containers, transfer line boxes.
        if (is<Layout::BlockContainer>(node)) {
            auto& block_container = static_cast<Layout::BlockContainer&>(node);
            block_container.set_line_boxes(move(node_state.line_boxes));
        }
    }
}

Gfx::FloatRect margin_box_rect(Box const& box, FormattingState const& state)
{
    auto const& box_state = state.get(box);
    auto rect = Gfx::FloatRect { box_state.offset, { box_state.content_width, box_state.content_height } };
    rect.set_x(rect.x() - box_state.margin_box_left());
    rect.set_width(rect.width() + box_state.margin_box_left() + box_state.margin_box_right());
    rect.set_y(rect.y() - box_state.margin_box_top());
    rect.set_height(rect.height() + box_state.margin_box_top() + box_state.margin_box_bottom());
    return rect;
}

Gfx::FloatRect margin_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, FormattingState const& state)
{
    auto rect = margin_box_rect(box, state);
    for (auto const* current = box.parent(); current; current = current->parent()) {
        if (current == &ancestor_box)
            break;
        if (is<Box>(*current)) {
            auto const& current_state = state.get(static_cast<Box const&>(*current));
            rect.translate_by(current_state.offset);
        }
    }
    return rect;
}

Gfx::FloatRect absolute_content_rect(Box const& box, FormattingState const& state)
{
    auto const& box_state = state.get(box);
    Gfx::FloatRect rect { box_state.offset, { box_state.content_width, box_state.content_height } };
    for (auto* block = box.containing_block(); block; block = block->containing_block())
        rect.translate_by(state.get(*block).offset);
    return rect;
}

}
