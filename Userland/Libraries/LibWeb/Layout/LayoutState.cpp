/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/AvailableSpace.h>
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

    auto const* containing_block_used_values = box.is_initial_containing_block_box() ? nullptr : &get(*box.containing_block());

    used_values_per_layout_node[serial_id] = adopt_own(*new UsedValues);
    used_values_per_layout_node[serial_id]->set_node(const_cast<NodeWithStyleAndBoxModelMetrics&>(box), containing_block_used_values);
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

    auto const* containing_block_used_values = box.is_initial_containing_block_box() ? nullptr : &get(*box.containing_block());

    const_cast<LayoutState*>(this)->used_values_per_layout_node[serial_id] = adopt_own(*new UsedValues);
    const_cast<LayoutState*>(this)->used_values_per_layout_node[serial_id]->set_node(const_cast<NodeWithStyleAndBoxModelMetrics&>(box), containing_block_used_values);
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
        auto& node = const_cast<NodeWithStyleAndBoxModelMetrics&>(used_values.node());

        // Transfer box model metrics.
        node.box_model().inset = { used_values.inset_top, used_values.inset_right, used_values.inset_bottom, used_values.inset_left };
        node.box_model().padding = { used_values.padding_top, used_values.padding_right, used_values.padding_bottom, used_values.padding_left };
        node.box_model().border = { used_values.border_top, used_values.border_right, used_values.border_bottom, used_values.border_left };
        node.box_model().margin = { used_values.margin_top, used_values.margin_right, used_values.margin_bottom, used_values.margin_left };

        node.set_paintable(node.create_paintable());

        // For boxes, transfer all the state needed for painting.
        if (is<Layout::Box>(node)) {
            auto& box = static_cast<Layout::Box const&>(node);
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

Gfx::FloatRect border_box_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    auto rect = Gfx::FloatRect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    rect.set_x(rect.x() - box_state.border_box_left());
    rect.set_width(rect.width() + box_state.border_box_left() + box_state.border_box_right());
    rect.set_y(rect.y() - box_state.border_box_top());
    rect.set_height(rect.height() + box_state.border_box_top() + box_state.border_box_bottom());
    return rect;
}

Gfx::FloatRect border_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
{
    auto rect = border_box_rect(box, state);
    if (&box == &ancestor_box)
        return rect;
    for (auto const* current = box.containing_block(); current; current = current->containing_block()) {
        if (current == &ancestor_box)
            return rect;
        auto const& current_state = state.get(static_cast<Box const&>(*current));
        rect.translate_by(current_state.offset);
    }
    // If we get here, ancestor_box was not a containing block ancestor of `box`!
    VERIFY_NOT_REACHED();
}

Gfx::FloatRect content_box_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    return Gfx::FloatRect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
}

Gfx::FloatRect content_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
{
    auto rect = content_box_rect(box, state);
    if (&box == &ancestor_box)
        return rect;
    for (auto const* current = box.containing_block(); current; current = current->containing_block()) {
        if (current == &ancestor_box)
            return rect;
        auto const& current_state = state.get(static_cast<Box const&>(*current));
        rect.translate_by(current_state.offset);
    }
    // If we get here, ancestor_box was not a containing block ancestor of `box`!
    VERIFY_NOT_REACHED();
}

Gfx::FloatRect margin_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
{
    auto rect = margin_box_rect(box, state);
    if (&box == &ancestor_box)
        return rect;
    for (auto const* current = box.containing_block(); current; current = current->containing_block()) {
        if (current == &ancestor_box)
            return rect;
        auto const& current_state = state.get(static_cast<Box const&>(*current));
        rect.translate_by(current_state.offset);
    }
    // If we get here, ancestor_box was not a containing block ancestor of `box`!
    VERIFY_NOT_REACHED();
}

Gfx::FloatRect absolute_content_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    Gfx::FloatRect rect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    for (auto* block = box.containing_block(); block; block = block->containing_block())
        rect.translate_by(state.get(*block).offset);
    return rect;
}

void LayoutState::UsedValues::set_node(NodeWithStyleAndBoxModelMetrics& node, UsedValues const* containing_block_used_values)
{
    m_node = &node;

    auto const& computed_values = node.computed_values();

    auto is_definite_size = [&](CSS::Size const& size, float& resolved_definite_size, bool width) {
        // A size that can be determined without performing layout; that is,
        // a <length>,
        // a measure of text (without consideration of line-wrapping),
        // a size of the initial containing block,
        // or a <percentage> or other formula (such as the “stretch-fit” sizing of non-replaced blocks [CSS2]) that is resolved solely against definite sizes.

        auto containing_block_has_definite_size = containing_block_used_values ? (width ? containing_block_used_values->has_definite_width() : containing_block_used_values->has_definite_height()) : false;

        if (size.is_auto()) {
            // NOTE: The width of a non-flex-item block is considered definite if it's auto and the containing block has definite width.
            if (width
                && !node.is_floating()
                && node.display().is_block_outside()
                && node.parent()
                && !node.parent()->is_floating()
                && (node.parent()->display().is_flow_root_inside()
                    || node.parent()->display().is_flow_inside())) {
                if (containing_block_has_definite_size) {
                    float available_width = containing_block_used_values->content_width();
                    resolved_definite_size = available_width
                        - margin_left
                        - margin_right
                        - padding_left
                        - padding_right
                        - border_left
                        - border_right;
                    return true;
                }
                return false;
            }
            return false;
        }

        if (size.is_length() && size.length().is_calculated()) {
            if (width && size.length().calculated_style_value()->contains_percentage() && containing_block_has_definite_size) {
                auto& calc_value = *size.length().calculated_style_value();
                auto containing_block_width_as_length = CSS::Length::make_px(containing_block_used_values->content_width());
                resolved_definite_size = calc_value.resolve_length_percentage(node, containing_block_width_as_length).value_or(CSS::Length::make_auto()).to_px(node);
                return false;
            }
            if (size.length().calculated_style_value()->contains_percentage())
                return false;
            resolved_definite_size = size.length().to_px(node);
            return true;
        }

        if (size.is_length()) {
            VERIFY(!size.is_auto());                // This should have been covered by the Size::is_auto() branch above.
            VERIFY(!size.length().is_calculated()); // Covered above.
            resolved_definite_size = size.length().to_px(node);
            return true;
        }
        if (size.is_percentage()) {
            if (containing_block_has_definite_size) {
                auto containing_block_size = width ? containing_block_used_values->content_width() : containing_block_used_values->content_height();
                resolved_definite_size = containing_block_size * size.percentage().as_fraction();
                return true;
            }
            return false;
        }
        // FIXME: Determine if calc() value is definite.
        return false;
    };

    m_has_definite_width = is_definite_size(computed_values.width(), m_content_width, true);
    m_has_definite_height = is_definite_size(computed_values.height(), m_content_height, false);
}

void LayoutState::UsedValues::set_content_width(float width)
{
    m_content_width = width;
    m_has_definite_width = true;
}

void LayoutState::UsedValues::set_content_height(float height)
{
    m_content_height = height;
    m_has_definite_height = true;
}

float LayoutState::resolved_definite_width(Box const& box) const
{
    return get(box).content_width();
}

float LayoutState::resolved_definite_height(Box const& box) const
{
    return get(box).content_height();
}

AvailableSize LayoutState::UsedValues::available_width_inside() const
{
    if (width_constraint == SizeConstraint::MinContent)
        return AvailableSize::make_min_content();
    if (width_constraint == SizeConstraint::MaxContent)
        return AvailableSize::make_max_content();
    if (has_definite_width())
        return AvailableSize::make_definite(m_content_width);
    return AvailableSize::make_indefinite();
}

AvailableSize LayoutState::UsedValues::available_height_inside() const
{
    if (height_constraint == SizeConstraint::MinContent)
        return AvailableSize::make_min_content();
    if (height_constraint == SizeConstraint::MaxContent)
        return AvailableSize::make_max_content();
    if (has_definite_height())
        return AvailableSize::make_definite(m_content_height);
    return AvailableSize::make_indefinite();
}

AvailableSpace LayoutState::UsedValues::available_inner_space_or_constraints_from(AvailableSpace const& outer_space) const
{
    auto inner_width = available_width_inside();
    auto inner_height = available_height_inside();

    if (inner_width.is_indefinite() && outer_space.width.is_intrinsic_sizing_constraint())
        inner_width = outer_space.width;
    if (inner_height.is_indefinite() && outer_space.height.is_intrinsic_sizing_constraint())
        inner_height = outer_space.height;
    return AvailableSpace(inner_width, inner_height);
}

void LayoutState::UsedValues::set_content_offset(Gfx::FloatPoint offset)
{
    set_content_x(offset.x());
    set_content_y(offset.y());
}

void LayoutState::UsedValues::set_content_x(float x)
{
    offset.set_x(x);
}

void LayoutState::UsedValues::set_content_y(float y)
{
    offset.set_y(y);
}

}
