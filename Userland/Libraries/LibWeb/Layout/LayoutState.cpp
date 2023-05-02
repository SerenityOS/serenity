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

    auto const* containing_block_used_values = box.is_viewport() ? nullptr : &get(*box.containing_block());

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

    auto const* containing_block_used_values = box.is_viewport() ? nullptr : &get(*box.containing_block());

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
        node.box_model().inset = { used_values.inset_top.value(), used_values.inset_right.value(), used_values.inset_bottom.value(), used_values.inset_left.value() };
        node.box_model().padding = { used_values.padding_top.value(), used_values.padding_right.value(), used_values.padding_bottom.value(), used_values.padding_left.value() };
        node.box_model().border = { used_values.border_top.value(), used_values.border_right.value(), used_values.border_bottom.value(), used_values.border_left.value() };
        node.box_model().margin = { used_values.margin_top.value(), used_values.margin_right.value(), used_values.margin_bottom.value(), used_values.margin_left.value() };

        node.set_paintable(node.create_paintable());

        // For boxes, transfer all the state needed for painting.
        if (is<Layout::Box>(node)) {
            auto& box = static_cast<Layout::Box const&>(node);
            auto& paintable_box = const_cast<Painting::PaintableBox&>(*box.paintable_box());
            paintable_box.set_offset(used_values.offset);
            paintable_box.set_content_size(used_values.content_width(), used_values.content_height());
            paintable_box.set_overflow_data(move(used_values.overflow_data));
            paintable_box.set_containing_line_box_fragment(used_values.containing_line_box_fragment);

            if (is<Layout::BlockContainer>(box)) {
                for (auto& line_box : used_values.line_boxes) {
                    for (auto& fragment : line_box.fragments()) {
                        if (fragment.layout_node().is_text_node())
                            text_nodes.set(static_cast<Layout::TextNode*>(const_cast<Layout::Node*>(&fragment.layout_node())));
                    }
                }
                static_cast<Painting::PaintableWithLines&>(paintable_box).set_line_boxes(move(used_values.line_boxes));
            }
        }
    }

    for (auto* text_node : text_nodes)
        text_node->set_paintable(text_node->create_paintable());
}

CSSPixels box_baseline(LayoutState const& state, Box const& box)
{
    auto const& box_state = state.get(box);

    // https://www.w3.org/TR/CSS2/visudet.html#propdef-vertical-align
    auto const& vertical_align = box.computed_values().vertical_align();
    if (vertical_align.has<CSS::VerticalAlign>()) {
        switch (vertical_align.get<CSS::VerticalAlign>()) {
        case CSS::VerticalAlign::Top:
            // Top: Align the top of the aligned subtree with the top of the line box.
            return box_state.border_box_top();
        case CSS::VerticalAlign::Bottom:
            // Bottom: Align the bottom of the aligned subtree with the bottom of the line box.
            return box_state.content_height() + box_state.margin_box_top();
        case CSS::VerticalAlign::TextTop:
            // TextTop: Align the top of the box with the top of the parent's content area (see 10.6.1).
            return box.computed_values().font_size();
        case CSS::VerticalAlign::TextBottom:
            // TextTop: Align the bottom of the box with the bottom of the parent's content area (see 10.6.1).
            return box_state.content_height() - (box.containing_block()->font().pixel_metrics().descent * 2);
        default:
            break;
        }
    }

    if (!box_state.line_boxes.is_empty())
        return box_state.margin_box_top() + box_state.offset.y() + box_state.line_boxes.last().baseline();
    if (box.has_children() && !box.children_are_inline()) {
        auto const* child_box = box.last_child_of_type<Box>();
        VERIFY(child_box);
        return box_baseline(state, *child_box);
    }
    return box_state.margin_box_height();
}

CSSPixelRect margin_box_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    auto rect = CSSPixelRect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    rect.set_x(rect.x() - box_state.margin_box_left());
    rect.set_width(rect.width() + box_state.margin_box_left() + box_state.margin_box_right());
    rect.set_y(rect.y() - box_state.margin_box_top());
    rect.set_height(rect.height() + box_state.margin_box_top() + box_state.margin_box_bottom());
    return rect;
}

CSSPixelRect border_box_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    auto rect = CSSPixelRect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    rect.set_x(rect.x() - box_state.border_box_left());
    rect.set_width(rect.width() + box_state.border_box_left() + box_state.border_box_right());
    rect.set_y(rect.y() - box_state.border_box_top());
    rect.set_height(rect.height() + box_state.border_box_top() + box_state.border_box_bottom());
    return rect;
}

CSSPixelRect border_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
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

CSSPixelRect content_box_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    return CSSPixelRect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
}

CSSPixelRect content_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
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

CSSPixelRect margin_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const& state)
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

CSSPixelRect absolute_content_rect(Box const& box, LayoutState const& state)
{
    auto const& box_state = state.get(box);
    CSSPixelRect rect { box_state.offset, { box_state.content_width(), box_state.content_height() } };
    for (auto* block = box.containing_block(); block; block = block->containing_block())
        rect.translate_by(state.get(*block).offset);
    return rect;
}

void LayoutState::UsedValues::set_node(NodeWithStyleAndBoxModelMetrics& node, UsedValues const* containing_block_used_values)
{
    m_node = &node;

    // NOTE: In the code below, we decide if `node` has definite width and/or height.
    //       This attempts to cover all the *general* cases where CSS considers sizes to be definite.
    //       If `node` has definite values for min/max-width or min/max-height and a definite
    //       preferred size in the same axis, we clamp the preferred size here as well.
    //
    //       There are additional cases where CSS considers values to be definite. We model all of
    //       those by having our engine consider sizes to be definite *once they are assigned to
    //       the UsedValues by calling set_content_width() or set_content_height().

    auto const& computed_values = node.computed_values();

    auto is_definite_size = [&](CSS::Size const& size, CSSPixels& resolved_definite_size, bool width) {
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
                    CSSPixels available_width = containing_block_used_values->content_width();
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

        if (size.is_calculated()) {
            if (size.calculated().contains_percentage()) {
                if (!containing_block_has_definite_size)
                    return false;
                auto containing_block_size_as_length = width
                    ? CSS::Length::make_px(containing_block_used_values->content_width())
                    : CSS::Length::make_px(containing_block_used_values->content_height());
                resolved_definite_size = size.calculated().resolve_length_percentage(node, containing_block_size_as_length).value_or(CSS::Length::make_auto()).to_px(node);
                return true;
            }
            resolved_definite_size = size.calculated().resolve_length(node)->to_px(node);
            return true;
        }

        if (size.is_length()) {
            VERIFY(!size.is_auto()); // This should have been covered by the Size::is_auto() branch above.
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

    CSSPixels min_width = 0;
    bool has_definite_min_width = is_definite_size(computed_values.min_width(), min_width, true);
    CSSPixels max_width = 0;
    bool has_definite_max_width = is_definite_size(computed_values.max_width(), max_width, true);

    CSSPixels min_height = 0;
    bool has_definite_min_height = is_definite_size(computed_values.min_height(), min_height, false);
    CSSPixels max_height = 0;
    bool has_definite_max_height = is_definite_size(computed_values.max_height(), max_height, false);

    m_has_definite_width = is_definite_size(computed_values.width(), m_content_width, true);
    m_has_definite_height = is_definite_size(computed_values.height(), m_content_height, false);

    if (m_has_definite_width) {
        if (has_definite_min_width)
            m_content_width = max(min_width, m_content_width);
        if (has_definite_max_width)
            m_content_width = min(max_width, m_content_width);
    }

    if (m_has_definite_height) {
        if (has_definite_min_height)
            m_content_height = max(min_height, m_content_height);
        if (has_definite_max_height)
            m_content_height = min(max_height, m_content_height);
    }
}

void LayoutState::UsedValues::set_content_width(CSSPixels width)
{
    if (width < 0) {
        // Negative widths are not allowed in CSS. We have a bug somewhere! Clamp to 0 to avoid doing too much damage.
        dbgln("FIXME: Layout calculated a negative width for {}: {}", m_node->debug_description(), width);
        width = 0;
    }
    m_content_width = width;
    m_has_definite_width = true;
}

void LayoutState::UsedValues::set_content_height(CSSPixels height)
{
    if (height < 0) {
        // Negative heights are not allowed in CSS. We have a bug somewhere! Clamp to 0 to avoid doing too much damage.
        dbgln("FIXME: Layout calculated a negative height for {}: {}", m_node->debug_description(), height);
        height = 0;
    }
    m_content_height = height;
    m_has_definite_height = true;
}

void LayoutState::UsedValues::set_temporary_content_width(CSSPixels width)
{
    m_content_width = width;
}

void LayoutState::UsedValues::set_temporary_content_height(CSSPixels height)
{
    m_content_height = height;
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

void LayoutState::UsedValues::set_content_offset(CSSPixelPoint new_offset)
{
    set_content_x(new_offset.x());
    set_content_y(new_offset.y());
}

void LayoutState::UsedValues::set_content_x(CSSPixels x)
{
    offset.set_x(x);
}

void LayoutState::UsedValues::set_content_y(CSSPixels y)
{
    offset.set_y(y);
}

void LayoutState::UsedValues::set_indefinite_content_width()
{
    m_has_definite_width = false;
}

void LayoutState::UsedValues::set_indefinite_content_height()
{
    m_has_definite_height = false;
}

}
