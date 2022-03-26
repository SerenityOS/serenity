/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingState.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

FormattingState::NodeState& FormattingState::get_mutable(NodeWithStyleAndBoxModelMetrics const& box)
{
    if (auto it = nodes.find(&box); it != nodes.end())
        return *it->value;

    for (auto* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (auto it = ancestor->nodes.find(&box); it != ancestor->nodes.end()) {
            auto cow_node_state = adopt_own(*new NodeState(*it->value));
            auto* cow_node_state_ptr = cow_node_state.ptr();
            nodes.set(&box, move(cow_node_state));
            return *cow_node_state_ptr;
        }
    }

    return *nodes.ensure(&box, [] { return adopt_own(*new NodeState); });
}

FormattingState::NodeState const& FormattingState::get(NodeWithStyleAndBoxModelMetrics const& box) const
{
    if (auto it = nodes.find(&box); it != nodes.end())
        return *it->value;

    for (auto* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (auto it = ancestor->nodes.find(&box); it != ancestor->nodes.end())
            return *it->value;
    }

    return *const_cast<FormattingState&>(*this).nodes.ensure(&box, [] { return adopt_own(*new NodeState); });
}

void FormattingState::commit()
{
    // Only the top-level FormattingState should ever be committed.
    VERIFY(!m_parent);

    HashTable<Layout::TextNode*> text_nodes;

    for (auto& it : nodes) {
        auto& node = const_cast<Layout::NodeWithStyleAndBoxModelMetrics&>(*it.key);
        auto& node_state = *it.value;

        // Transfer box model metrics.
        node.box_model().inset = { node_state.inset_top, node_state.inset_right, node_state.inset_bottom, node_state.inset_left };
        node.box_model().padding = { node_state.padding_top, node_state.padding_right, node_state.padding_bottom, node_state.padding_left };
        node.box_model().border = { node_state.border_top, node_state.border_right, node_state.border_bottom, node_state.border_left };
        node.box_model().margin = { node_state.margin_top, node_state.margin_right, node_state.margin_bottom, node_state.margin_left };

        node.set_paintable(node.create_paintable());

        // For boxes, transfer all the state needed for painting.
        if (is<Layout::Box>(node)) {
            auto& box = static_cast<Layout::Box&>(node);
            auto& paint_box = const_cast<Painting::PaintableBox&>(*box.paint_box());
            paint_box.set_offset(node_state.offset);
            paint_box.set_content_size(node_state.content_width, node_state.content_height);
            paint_box.set_overflow_data(move(node_state.overflow_data));
            paint_box.set_containing_line_box_fragment(node_state.containing_line_box_fragment);

            if (is<Layout::BlockContainer>(box)) {
                for (auto& line_box : node_state.line_boxes) {
                    for (auto& fragment : line_box.fragments()) {
                        if (fragment.layout_node().is_text_node())
                            text_nodes.set(static_cast<Layout::TextNode*>(const_cast<Layout::Node*>(&fragment.layout_node())));
                    }
                }
                static_cast<Painting::PaintableWithLines&>(paint_box).set_line_boxes(move(node_state.line_boxes));
            }
        }
    }

    for (auto* text_node : text_nodes)
        text_node->set_paintable(text_node->create_paintable());
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
