/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/LayoutState.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

LayoutState::UsedValues& LayoutState::get_mutable(NodeWithStyleAndBoxModelMetrics const& box)
{
    auto serial_id = box.serial_id();
    if (used_values_per_layout_node[serial_id])
        return *used_values_per_layout_node[serial_id];

    for (auto const* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (ancestor->used_values_per_layout_node[serial_id]) {
            auto cow_used_values = adopt_own(*new UsedValues(*ancestor->used_values_per_layout_node[serial_id]));
            auto* cow_used_values_ptr = cow_used_values.ptr();
            used_values_per_layout_node[serial_id] = move(cow_used_values);
            return *cow_used_values_ptr;
        }
    }

    used_values_per_layout_node[serial_id] = adopt_own(*new UsedValues);
    used_values_per_layout_node[serial_id]->node = const_cast<NodeWithStyleAndBoxModelMetrics*>(&box);
    return *used_values_per_layout_node[serial_id];
}

LayoutState::UsedValues const& LayoutState::get(NodeWithStyleAndBoxModelMetrics const& box) const
{
    auto serial_id = box.serial_id();
    if (used_values_per_layout_node[serial_id])
        return *used_values_per_layout_node[serial_id];

    for (auto* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (ancestor->used_values_per_layout_node[serial_id])
            return *ancestor->used_values_per_layout_node[serial_id];
    }
    const_cast<LayoutState*>(this)->used_values_per_layout_node[serial_id] = adopt_own(*new UsedValues);
    const_cast<LayoutState*>(this)->used_values_per_layout_node[serial_id]->node = const_cast<NodeWithStyleAndBoxModelMetrics*>(&box);
    return *used_values_per_layout_node[serial_id];
}

void LayoutState::commit()
{
    // Only the top-level LayoutState should ever be committed.
    VERIFY(!m_parent);

    HashTable<Layout::TextNode*> text_nodes;

    for (auto& used_values_ptr : used_values_per_layout_node) {
        if (!used_values_ptr)
            continue;
        auto& used_values = *used_values_ptr;
        auto& node = *used_values.node;

        // Transfer box model metrics.
        node.box_model().inset = { used_values.inset_top, used_values.inset_right, used_values.inset_bottom, used_values.inset_left };
        node.box_model().padding = { used_values.padding_top, used_values.padding_right, used_values.padding_bottom, used_values.padding_left };
        node.box_model().border = { used_values.border_top, used_values.border_right, used_values.border_bottom, used_values.border_left };
        node.box_model().margin = { used_values.margin_top, used_values.margin_right, used_values.margin_bottom, used_values.margin_left };

        node.set_paintable(node.create_paintable());

        // For boxes, transfer all the state needed for painting.
        if (is<Layout::Box>(node)) {
            auto& box = static_cast<Layout::Box&>(node);
            auto& paint_box = const_cast<Painting::PaintableBox&>(*box.paint_box());
            paint_box.set_offset(used_values.offset);
            paint_box.set_content_size(used_values.content_width(), used_values.content_height());
            paint_box.set_overflow_data(move(used_values.overflow_data));
            paint_box.set_containing_line_box_fragment(used_values.containing_line_box_fragment);

            if (is<Layout::BlockContainer>(box)) {
                for (auto& line_box : used_values.line_boxes) {
                    for (auto& fragment : line_box.fragments()) {
                        if (fragment.layout_node().is_text_node())
                            text_nodes.set(static_cast<Layout::TextNode*>(const_cast<Layout::Node*>(&fragment.layout_node())));
                    }
                }
                static_cast<Painting::PaintableWithLines&>(paint_box).set_line_boxes(move(used_values.line_boxes));
            }
        }
    }

    for (auto* text_node : text_nodes)
        text_node->set_paintable(text_node->create_paintable());
}

Gfx::FloatRect margin_box_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    auto rect = Gfx::FloatRect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    rect.set_x(rect.x() - box_state.margin_box_left());
    rect.set_width(rect.width() + box_state.margin_box_left() + box_state.margin_box_right());
    rect.set_y(rect.y() - box_state.margin_box_top());
    rect.set_height(rect.height() + box_state.margin_box_top() + box_state.margin_box_bottom());
    return rect;
}

Gfx::FloatRect margin_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
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

Gfx::FloatRect absolute_content_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    Gfx::FloatRect rect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    for (auto* block = box.containing_block(); block; block = block->containing_block())
        rect.translate_by(state.get(*block).offset);
    return rect;
}

void LayoutState::UsedValues::set_content_width(float width)
{
    m_content_width = width;
}

void LayoutState::UsedValues::set_content_height(float height)
{
    m_content_height = height;
}

}
