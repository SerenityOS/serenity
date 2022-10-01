/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/LineBuilder.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

BlockFormattingContext::BlockFormattingContext(LayoutState& state, BlockContainer const& root, FormattingContext* parent)
    : FormattingContext(Type::Block, state, root, parent)
{
}

BlockFormattingContext::~BlockFormattingContext()
{
    if (!m_was_notified_after_parent_dimensioned_my_root_box) {
        // HACK: The parent formatting context never notified us after assigning dimensions to our root box.
        //       Pretend that it did anyway, to make sure absolutely positioned children get laid out.
        // FIXME: Get rid of this hack once parent contexts behave properly.
        parent_context_did_dimension_child_root_box();
    }
}

bool BlockFormattingContext::is_initial() const
{
    return is<InitialContainingBlock>(root());
}

float BlockFormattingContext::automatic_content_height() const
{
    return compute_auto_height_for_block_formatting_context_root(root());
}

void BlockFormattingContext::run(Box const&, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    if (is_initial()) {
        layout_initial_containing_block(layout_mode, available_space);
        return;
    }

    if (root().children_are_inline())
        layout_inline_children(root(), layout_mode, available_space);
    else
        layout_block_level_children(root(), layout_mode, available_space);
}

void BlockFormattingContext::parent_context_did_dimension_child_root_box()
{
    m_was_notified_after_parent_dimensioned_my_root_box = true;

    // Left-side floats: offset_from_edge is from left edge (0) to left content edge of floating_box.
    for (auto& floating_box : m_left_floats.all_boxes) {
        auto& box_state = m_state.get_mutable(floating_box->box);
        box_state.set_content_x(floating_box->offset_from_edge);
    }

    // Right-side floats: offset_from_edge is from right edge (float_containing_block_width) to the left content edge of floating_box.
    for (auto& floating_box : m_right_floats.all_boxes) {
        auto float_containing_block_width = containing_block_width_for(floating_box->box);
        auto& box_state = m_state.get_mutable(floating_box->box);
        box_state.set_content_x(float_containing_block_width - floating_box->offset_from_edge);
    }

    // We can also layout absolutely positioned boxes within this BFC.
    for (auto& box : m_absolutely_positioned_boxes) {
        auto& cb_state = m_state.get(*box.containing_block());
        auto available_width = AvailableSize::make_definite(cb_state.content_width() + cb_state.padding_left + cb_state.padding_right);
        auto available_height = AvailableSize::make_definite(cb_state.content_height() + cb_state.padding_top + cb_state.padding_bottom);
        layout_absolutely_positioned_element(box, AvailableSpace(available_width, available_height));
    }
}

void BlockFormattingContext::compute_width(Box const& box, AvailableSpace const& available_space, LayoutMode)
{
    if (box.is_absolutely_positioned()) {
        compute_width_for_absolutely_positioned_element(box, available_space);
        return;
    }

    if (is<ReplacedBox>(box)) {
        // FIXME: This should not be done *by* ReplacedBox
        auto& replaced = verify_cast<ReplacedBox>(box);
        // FIXME: This const_cast is gross.
        const_cast<ReplacedBox&>(replaced).prepare_for_replaced_layout();
        compute_width_for_block_level_replaced_element_in_normal_flow(replaced, available_space);
        // NOTE: We don't return here.
    }

    if (box.is_floating()) {
        compute_width_for_floating_box(box, available_space);
        return;
    }

    auto const& computed_values = box.computed_values();

    if (should_treat_width_as_auto(box, available_space) && available_space.width.is_intrinsic_sizing_constraint())
        return;

    float width_of_containing_block = available_space.width.to_px();
    auto width_of_containing_block_as_length_for_resolve = available_space.width.is_definite() ? CSS::Length::make_px(width_of_containing_block) : CSS::Length::make_px(0);

    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    auto padding_left = computed_values.padding().left().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
    auto padding_right = computed_values.padding().right().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);

    auto try_compute_width = [&](auto const& a_width) {
        CSS::Length width = a_width;
        margin_left = computed_values.margin().left().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
        margin_right = computed_values.margin().right().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
        float total_px = computed_values.border_left().width + computed_values.border_right().width;
        for (auto& value : { margin_left, padding_left, width, padding_right, margin_right }) {
            total_px += value.to_px(box);
        }

        if (!box.is_inline()) {
            // 10.3.3 Block-level, non-replaced elements in normal flow
            // If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
            if (width.is_auto() && total_px > width_of_containing_block) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
            }

            // 10.3.3 cont'd.
            auto underflow_px = width_of_containing_block - total_px;
            if (!isfinite(underflow_px))
                underflow_px = 0;

            if (width.is_auto()) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;

                if (available_space.width.is_definite()) {
                    if (underflow_px >= 0) {
                        width = CSS::Length(underflow_px, CSS::Length::Type::Px);
                    } else {
                        width = zero_value;
                        margin_right = CSS::Length(margin_right.to_px(box) + underflow_px, CSS::Length::Type::Px);
                    }
                }
            } else {
                if (!margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_right = CSS::Length(margin_right.to_px(box) + underflow_px, CSS::Length::Type::Px);
                } else if (!margin_left.is_auto() && margin_right.is_auto()) {
                    margin_right = CSS::Length(underflow_px, CSS::Length::Type::Px);
                } else if (margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_left = CSS::Length(underflow_px, CSS::Length::Type::Px);
                } else { // margin_left.is_auto() && margin_right.is_auto()
                    auto half_of_the_underflow = CSS::Length(underflow_px / 2, CSS::Length::Type::Px);
                    margin_left = half_of_the_underflow;
                    margin_right = half_of_the_underflow;
                }
            }
        }

        return width;
    };

    auto input_width = [&] {
        if (should_treat_width_as_auto(box, available_space))
            return CSS::Length::make_auto();
        return computed_values.width().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
    }();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(input_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    if (!computed_values.max_width().is_none()) {
        auto max_width = computed_values.max_width().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
        if (used_width.to_px(box) > max_width.to_px(box)) {
            used_width = try_compute_width(max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    if (!computed_values.min_width().is_auto()) {
        auto min_width = computed_values.min_width().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
        if (used_width.to_px(box) < min_width.to_px(box)) {
            used_width = try_compute_width(min_width);
        }
    }

    auto& box_state = m_state.get_mutable(box);

    if (!is<ReplacedBox>(box) && !used_width.is_auto())
        box_state.set_content_width(used_width.to_px(box));

    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left.to_px(box);
    box_state.padding_right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_floating_box(Box const& box, AvailableSpace const& available_space)
{
    // 10.3.5 Floating, non-replaced elements
    auto& computed_values = box.computed_values();

    auto zero_value = CSS::Length::make_px(0);
    float width_of_containing_block = available_space.width.to_px();
    auto width_of_containing_block_as_length_for_resolve = CSS::Length::make_px(width_of_containing_block);
    if (!available_space.width.is_definite())
        width_of_containing_block_as_length_for_resolve = CSS::Length::make_px(0);

    auto margin_left = computed_values.margin().left().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
    auto margin_right = computed_values.margin().right().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
    auto const padding_left = computed_values.padding().left().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
    auto const padding_right = computed_values.padding().right().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto compute_width = [&](auto width) {
        // If 'width' is computed as 'auto', the used value is the "shrink-to-fit" width.
        if (width.is_auto()) {

            // Find the available width: in this case, this is the width of the containing
            // block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
            // 'padding-right', 'border-right-width', 'margin-right', and the widths of any relevant scroll bars.
            float available_width = width_of_containing_block
                - margin_left.to_px(box) - computed_values.border_left().width - padding_left.to_px(box)
                - padding_right.to_px(box) - computed_values.border_right().width - margin_right.to_px(box);

            auto result = calculate_shrink_to_fit_widths(box);

            // Then the shrink-to-fit width is: min(max(preferred minimum width, available width), preferred width).
            width = CSS::Length(min(max(result.preferred_minimum_width, available_width), result.preferred_width), CSS::Length::Type::Px);
        }

        return width;
    };

    auto input_width = [&] {
        if (should_treat_width_as_auto(box, available_space))
            return CSS::Length::make_auto();
        return computed_values.width().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
    }();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto width = compute_width(input_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    if (!computed_values.max_width().is_none()) {
        auto max_width = computed_values.max_width().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
        if (width.to_px(box) > max_width.to_px(box))
            width = compute_width(max_width);
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    if (!computed_values.min_width().is_auto()) {
        auto min_width = computed_values.min_width().resolved(box, width_of_containing_block_as_length_for_resolve).resolved(box);
        if (width.to_px(box) < min_width.to_px(box))
            width = compute_width(min_width);
    }

    auto& box_state = m_state.get_mutable(box);
    box_state.set_content_width(width.to_px(box));
    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left.to_px(box);
    box_state.padding_right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox const& box, AvailableSpace const& available_space)
{
    m_state.get_mutable(box).set_content_width(compute_width_for_replaced_element(m_state, box, available_space));
}

void BlockFormattingContext::compute_height(Box const& box, AvailableSpace const& available_space)
{
    resolve_vertical_box_model_metrics(box, available_space, m_state);

    auto const& computed_values = box.computed_values();
    auto containing_block_height = CSS::Length::make_px(available_space.height.to_px());

    // Then work out what the height is, based on box type and CSS properties.
    float height = 0;
    if (is<ReplacedBox>(box)) {
        height = compute_height_for_replaced_element(m_state, verify_cast<ReplacedBox>(box), available_space);
    } else {
        if (should_treat_height_as_auto(box, available_space)) {
            height = compute_auto_height_for_block_level_element(box);
        } else {
            height = computed_values.height().resolved(box, containing_block_height).to_px(box);
        }
    }

    if (!computed_values.max_height().is_none()) {
        auto max_height = computed_values.max_height().resolved(box, containing_block_height).resolved(box);
        if (!max_height.is_auto())
            height = min(height, max_height.to_px(box));
    }
    if (!computed_values.min_height().is_auto()) {
        auto min_height = computed_values.min_height().resolved(box, containing_block_height).resolved(box);
        height = max(height, min_height.to_px(box));
    }

    m_state.get_mutable(box).set_content_height(height);
}

void BlockFormattingContext::layout_inline_children(BlockContainer const& block_container, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    VERIFY(block_container.children_are_inline());

    auto& block_container_state = m_state.get_mutable(block_container);

    InlineFormattingContext context(m_state, block_container, *this);
    context.run(
        block_container,
        layout_mode,
        available_space);

    if (!block_container_state.has_definite_width())
        block_container_state.set_content_width(context.automatic_content_width());
    if (!block_container_state.has_definite_height())
        block_container_state.set_content_height(context.automatic_content_height());
}

void BlockFormattingContext::layout_block_level_box(Box const& box, BlockContainer const& block_container, LayoutMode layout_mode, float& bottom_of_lowest_margin_box, AvailableSpace const& available_space)
{
    auto& box_state = m_state.get_mutable(box);

    if (box.is_absolutely_positioned()) {
        m_absolutely_positioned_boxes.append(box);
        return;
    }

    // NOTE: ListItemMarkerBoxes are placed by their corresponding ListItemBox.
    if (is<ListItemMarkerBox>(box))
        return;

    if (box.is_floating()) {
        layout_floating_box(box, block_container, layout_mode, available_space);
        bottom_of_lowest_margin_box = max(bottom_of_lowest_margin_box, box_state.offset.y() + box_state.content_height() + box_state.margin_box_bottom());
        return;
    }

    compute_width(box, available_space, layout_mode);

    if (is<ReplacedBox>(box) || is<BlockContainer>(box))
        place_block_level_element_in_normal_flow_vertically(box, available_space);

    if (box_state.has_definite_height()) {
        compute_height(box, available_space);
    }

    OwnPtr<FormattingContext> independent_formatting_context;
    if (box.can_have_children()) {
        if (box.children_are_inline()) {
            layout_inline_children(verify_cast<BlockContainer>(box), layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
        } else {
            independent_formatting_context = create_independent_formatting_context_if_needed(m_state, box);
            if (independent_formatting_context)
                independent_formatting_context->run(box, layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
            else
                layout_block_level_children(verify_cast<BlockContainer>(box), layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
        }
    }

    compute_height(box, available_space);

    compute_inset(box);

    if (is<ReplacedBox>(box) || is<BlockContainer>(box))
        place_block_level_element_in_normal_flow_horizontally(box, available_space);

    if (is<ListItemBox>(box)) {
        layout_list_item_marker(static_cast<ListItemBox const&>(box));
    }

    bottom_of_lowest_margin_box = max(bottom_of_lowest_margin_box, box_state.offset.y() + box_state.content_height() + box_state.margin_box_bottom());

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void BlockFormattingContext::layout_block_level_children(BlockContainer const& block_container, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    VERIFY(!block_container.children_are_inline());

    float bottom_of_lowest_margin_box = 0;

    block_container.for_each_child_of_type<Box>([&](Box& box) {
        layout_block_level_box(box, block_container, layout_mode, bottom_of_lowest_margin_box, available_space);
        return IterationDecision::Continue;
    });

    if (layout_mode == LayoutMode::IntrinsicSizing) {
        auto& block_container_state = m_state.get_mutable(block_container);
        if (!block_container_state.has_definite_width())
            block_container_state.set_content_width(greatest_child_width(block_container));
        if (!block_container_state.has_definite_height())
            block_container_state.set_content_height(bottom_of_lowest_margin_box);
    }
}

void BlockFormattingContext::resolve_vertical_box_model_metrics(Box const& box, AvailableSpace const& available_space, LayoutState& state)
{
    auto& box_state = state.get_mutable(box);
    auto const& computed_values = box.computed_values();
    auto width_of_containing_block = CSS::Length::make_px(available_space.width.to_px_or_zero());

    box_state.margin_top = computed_values.margin().top().resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_bottom = computed_values.margin().bottom().resolved(box, width_of_containing_block).to_px(box);
    box_state.border_top = computed_values.border_top().width;
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.padding_top = computed_values.padding().top().resolved(box, width_of_containing_block).to_px(box);
    box_state.padding_bottom = computed_values.padding().bottom().resolved(box, width_of_containing_block).to_px(box);
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_vertically(Box const& child_box, AvailableSpace const& available_space)
{
    auto& box_state = m_state.get_mutable(child_box);
    auto const& computed_values = child_box.computed_values();

    resolve_vertical_box_model_metrics(child_box, available_space, m_state);

    auto y = FormattingContext::compute_box_y_position_with_respect_to_siblings(child_box);

    auto clear_floating_boxes = [&](FloatSideData& float_side) {
        if (!float_side.current_boxes.is_empty()) {
            // NOTE: Floating boxes are globally relevant within this BFC, *but* their offset coordinates
            //       are relative to their containing block.
            //       This means that we have to first convert to a root-space Y coordinate before clearing,
            //       and then convert back to a local Y coordinate when assigning the cleared offset to
            //       the `child_box` layout state.

            // First, find the lowest margin box edge on this float side and calculate the Y offset just below it.
            float clearance_y_in_root = 0;
            for (auto const& floating_box : float_side.current_boxes) {
                auto floating_box_rect_in_root = margin_box_rect_in_ancestor_coordinate_space(floating_box.box, root(), m_state);
                clearance_y_in_root = max(clearance_y_in_root, floating_box_rect_in_root.bottom() + 1);
            }

            // Then, convert the clearance Y to a coordinate relative to the containing block of `child_box`.
            float clearance_y_in_containing_block = clearance_y_in_root;
            for (auto* containing_block = child_box.containing_block(); containing_block && containing_block != &root(); containing_block = containing_block->containing_block())
                clearance_y_in_containing_block -= m_state.get(*containing_block).offset.y();

            y = max(y, clearance_y_in_containing_block);
            float_side.clear();
        }
    };

    // Flex-items don't float and also don't clear.
    if ((computed_values.clear() == CSS::Clear::Left || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_left_floats);
    if ((computed_values.clear() == CSS::Clear::Right || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_right_floats);

    box_state.set_content_offset(Gfx::FloatPoint { box_state.offset.x(), y });
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_horizontally(Box const& child_box, AvailableSpace const& available_space)
{
    auto& box_state = m_state.get_mutable(child_box);

    float x = 0;
    float available_width_within_containing_block = available_space.width.to_px();

    if ((!m_left_floats.current_boxes.is_empty() || !m_right_floats.current_boxes.is_empty())
        && creates_block_formatting_context(child_box)) {
        available_width_within_containing_block -= m_left_floats.current_width + m_right_floats.current_width;
        x += m_left_floats.current_width;
    }

    if (child_box.containing_block()->computed_values().text_align() == CSS::TextAlign::LibwebCenter) {
        x += (available_width_within_containing_block / 2) - box_state.content_width() / 2;
    } else {
        x += box_state.margin_box_left();
    }

    box_state.set_content_offset(Gfx::FloatPoint { x, box_state.offset.y() });
}

static void measure_scrollable_overflow(LayoutState const& state, Box const& box, float& bottom_edge, float& right_edge)
{
    auto const& child_state = state.get(box);
    auto child_rect = absolute_content_rect(box, state);
    child_rect.inflate(child_state.border_box_top(), child_state.border_box_right(), child_state.border_box_bottom(), child_state.border_box_left());

    bottom_edge = max(bottom_edge, child_rect.bottom());
    right_edge = max(right_edge, child_rect.right());

    if (box.computed_values().overflow_x() == CSS::Overflow::Hidden && box.computed_values().overflow_y() == CSS::Overflow::Hidden)
        return;

    box.for_each_child_of_type<Box>([&](Box const& child) {
        measure_scrollable_overflow(state, child, bottom_edge, right_edge);
        return IterationDecision::Continue;
    });
}

void BlockFormattingContext::layout_initial_containing_block(LayoutMode layout_mode, AvailableSpace const& available_space)
{
    auto viewport_rect = root().browsing_context().viewport_rect();

    auto& icb = verify_cast<Layout::InitialContainingBlock>(root());
    auto& icb_state = m_state.get_mutable(icb);

    if (root().children_are_inline())
        layout_inline_children(root(), layout_mode, available_space);
    else
        layout_block_level_children(root(), layout_mode, available_space);

    float bottom_edge = 0;
    float right_edge = 0;
    measure_scrollable_overflow(m_state, icb, bottom_edge, right_edge);

    if (bottom_edge >= viewport_rect.height() || right_edge >= viewport_rect.width()) {
        // FIXME: Move overflow data to LayoutState!
        auto& overflow_data = icb_state.ensure_overflow_data();
        overflow_data.scrollable_overflow_rect = viewport_rect.to_type<float>();
        // NOTE: The edges are *within* the rectangle, so we add 1 to get the width and height.
        overflow_data.scrollable_overflow_rect.set_size(right_edge + 1, bottom_edge + 1);
    }
}

void BlockFormattingContext::layout_floating_box(Box const& box, BlockContainer const&, LayoutMode layout_mode, AvailableSpace const& available_space, LineBuilder* line_builder)
{
    VERIFY(box.is_floating());

    auto& box_state = m_state.get_mutable(box);
    float width_of_containing_block = available_space.width.to_px();

    compute_width(box, available_space, layout_mode);
    if (auto independent_formatting_context = layout_inside(box, layout_mode, box_state.available_inner_space_or_constraints_from(available_space)))
        independent_formatting_context->parent_context_did_dimension_child_root_box();
    compute_height(box, available_space);

    // First we place the box normally (to get the right y coordinate.)
    // If we have a LineBuilder, we're in the middle of inline layout, otherwise this is block layout.
    if (line_builder) {
        auto y = line_builder->y_for_float_to_be_inserted_here(box);
        box_state.set_content_y(y + box_state.margin_box_top());
    } else {
        place_block_level_element_in_normal_flow_vertically(box, available_space);
        place_block_level_element_in_normal_flow_horizontally(box, available_space);
    }

    // Then we float it to the left or right.
    auto float_box = [&](FloatSide side, FloatSideData& side_data, FloatSideData& other_side_data) {
        float offset_from_edge = 0;
        auto float_to_edge = [&] {
            if (side == FloatSide::Left)
                offset_from_edge = box_state.margin_box_left();
            else
                offset_from_edge = box_state.content_width() + box_state.margin_box_right();
        };

        auto box_in_root_rect = content_box_rect_in_ancestor_coordinate_space(box, root(), m_state);
        float y_in_root = box_in_root_rect.y();
        float y = box_state.offset.y();

        if (side_data.current_boxes.is_empty()) {
            // This is the first floating box on this side. Go all the way to the edge.
            float_to_edge();
            side_data.y_offset = 0;
        } else {
            auto& previous_box = side_data.current_boxes.last();

            float wanted_offset_from_edge = 0;
            bool fits_on_line = false;

            if (side == FloatSide::Left) {
                wanted_offset_from_edge = side_data.current_width + box_state.margin_box_left();
                fits_on_line = (wanted_offset_from_edge + box_state.content_width() + box_state.margin_box_right()) <= width_of_containing_block;
            } else {
                wanted_offset_from_edge = side_data.current_width + box_state.margin_box_right() + box_state.content_width();
                fits_on_line = (wanted_offset_from_edge - box_state.margin_box_left()) >= 0;
            }

            if (fits_on_line) {
                auto const previous_rect = margin_box_rect_in_ancestor_coordinate_space(previous_box.box, root(), m_state);
                if (previous_rect.contains_vertically(y_in_root + side_data.y_offset)) {
                    // This box touches another already floating box. Stack after others.
                    offset_from_edge = wanted_offset_from_edge;
                } else {
                    // This box does not touch another floating box, go all the way to the edge.
                    float_to_edge();

                    // Also, forget all previous boxes floated to this side while since they're no longer relevant.
                    side_data.clear();
                }
            } else {
                // We ran out of horizontal space on this "float line", and need to break.
                float_to_edge();
                float lowest_margin_edge = 0;
                for (auto const& box : side_data.current_boxes) {
                    auto const& box_state = m_state.get(box.box);
                    lowest_margin_edge = max(lowest_margin_edge, box_state.margin_box_height());
                }

                side_data.y_offset += lowest_margin_edge;

                // Also, forget all previous boxes floated to this side while since they're no longer relevant.
                side_data.clear();
            }
        }

        // NOTE: If we're in inline layout, the LineBuilder has already provided the right Y offset.
        //       In block layout, we adjust by the side's current Y offset here.
        // FIXME: It's annoying that we have different behavior for inline vs block here.
        //        Find a way to unify the behavior so we don't need to branch here.

        if (!line_builder)
            y += side_data.y_offset;

        side_data.all_boxes.append(adopt_own(*new FloatingBox {
            .box = box,
            .offset_from_edge = offset_from_edge,
            .top_margin_edge = y - box_state.margin_box_top(),
            .bottom_margin_edge = y + box_state.content_height() + box_state.margin_box_bottom(),
        }));
        side_data.current_boxes.append(*side_data.all_boxes.last());

        if (side == FloatSide::Left) {
            side_data.current_width = offset_from_edge + box_state.content_width() + box_state.margin_box_right();
        } else {
            side_data.current_width = offset_from_edge + box_state.margin_box_left();
        }
        side_data.max_width = max(side_data.current_width, side_data.max_width);

        // NOTE: We don't set the X position here, that happens later, once we know the root block width.
        //       See parent_context_did_dimension_child_root_box() for that logic.
        box_state.set_content_y(y);

        // If the new box was inserted below the bottom of the opposite side,
        // we reset the other side back to its edge.
        if (y > other_side_data.y_offset)
            other_side_data.clear();
    };

    // Next, float to the left and/or right
    if (box.computed_values().float_() == CSS::Float::Left) {
        float_box(FloatSide::Left, m_left_floats, m_right_floats);
    } else if (box.computed_values().float_() == CSS::Float::Right) {
        float_box(FloatSide::Right, m_right_floats, m_left_floats);
    }

    m_state.get_mutable(root()).add_floating_descendant(box);

    if (line_builder)
        line_builder->recalculate_available_space();
}

void BlockFormattingContext::layout_list_item_marker(ListItemBox const& list_item_box)
{
    if (!list_item_box.marker())
        return;

    auto& marker = *list_item_box.marker();
    auto& marker_state = m_state.get_mutable(marker);
    auto& list_item_state = m_state.get_mutable(list_item_box);

    int image_width = 0;
    int image_height = 0;
    if (auto const* list_style_image = marker.list_style_image()) {
        image_width = list_style_image->natural_width().value_or(0);
        image_height = list_style_image->natural_height().value_or(0);
    }

    int default_marker_width = max(4, marker.font().glyph_height() - 4);

    if (marker.text().is_empty()) {
        marker_state.set_content_width(image_width + default_marker_width);
    } else {
        auto text_width = marker.font().width(marker.text());
        marker_state.set_content_width(image_width + text_width);
    }

    marker_state.set_content_height(max(image_height, marker.font().glyph_height() + 1));

    marker_state.set_content_offset({ -(marker_state.content_width() + default_marker_width),
        max(0.f, (marker.line_height() - marker_state.content_height()) / 2.f) });

    if (marker_state.content_height() > list_item_state.content_height())
        list_item_state.set_content_height(marker_state.content_height());
}

BlockFormattingContext::SpaceUsedByFloats BlockFormattingContext::space_used_by_floats(float y) const
{
    SpaceUsedByFloats space_used_by_floats;

    for (auto const& floating_box_ptr : m_left_floats.all_boxes.in_reverse()) {
        auto const& floating_box = *floating_box_ptr;
        auto const& floating_box_state = m_state.get(floating_box.box);
        // NOTE: The floating box is *not* in the final horizontal position yet, but the size and vertical position is valid.
        auto rect = margin_box_rect_in_ancestor_coordinate_space(floating_box.box, root(), m_state);
        if (rect.contains_vertically(y)) {
            space_used_by_floats.left = floating_box.offset_from_edge
                + floating_box_state.content_width()
                + floating_box_state.margin_box_right();
            break;
        }
    }

    for (auto const& floating_box_ptr : m_right_floats.all_boxes.in_reverse()) {
        auto const& floating_box = *floating_box_ptr;
        auto const& floating_box_state = m_state.get(floating_box.box);
        // NOTE: The floating box is *not* in the final horizontal position yet, but the size and vertical position is valid.
        auto rect = margin_box_rect_in_ancestor_coordinate_space(floating_box.box, root(), m_state);
        if (rect.contains_vertically(y)) {
            space_used_by_floats.right = floating_box.offset_from_edge
                + floating_box_state.margin_box_left();
            break;
        }
    }

    return space_used_by_floats;
}

float BlockFormattingContext::greatest_child_width(Box const& box)
{
    // Similar to FormattingContext::greatest_child_width()
    // but this one takes floats into account!
    float max_width = m_left_floats.max_width + m_right_floats.max_width;
    if (box.children_are_inline()) {
        for (auto const& line_box : m_state.get(verify_cast<BlockContainer>(box)).line_boxes) {
            auto width_here = line_box.width();
            float extra_width_from_left_floats = 0;
            for (auto& left_float : m_left_floats.all_boxes) {
                if (line_box.baseline() >= left_float->top_margin_edge || line_box.baseline() <= left_float->bottom_margin_edge) {
                    auto const& left_float_state = m_state.get(left_float->box);
                    extra_width_from_left_floats = max(extra_width_from_left_floats, left_float->offset_from_edge + left_float_state.content_width() + left_float_state.margin_box_right());
                }
            }
            float extra_width_from_right_floats = 0;
            for (auto& right_float : m_right_floats.all_boxes) {
                if (line_box.baseline() >= right_float->top_margin_edge || line_box.baseline() <= right_float->bottom_margin_edge) {
                    auto const& right_float_state = m_state.get(right_float->box);
                    extra_width_from_right_floats = max(extra_width_from_right_floats, right_float->offset_from_edge + right_float_state.margin_box_left());
                }
            }
            width_here += extra_width_from_left_floats + extra_width_from_right_floats;
            max_width = max(max_width, width_here);
        }
    } else {
        box.for_each_child_of_type<Box>([&](Box const& child) {
            if (!child.is_absolutely_positioned())
                max_width = max(max_width, m_state.get(child).border_box_width());
        });
    }
    return max_width;
}

bool BlockFormattingContext::should_treat_width_as_auto(Box const& box, AvailableSpace const& available_space)
{
    return box.computed_values().width().is_auto()
        || (box.computed_values().width().contains_percentage() && !available_space.width.is_definite());
}

bool BlockFormattingContext::should_treat_height_as_auto(Box const& box, AvailableSpace const& available_space)
{
    return box.computed_values().height().is_auto()
        || (box.computed_values().height().contains_percentage() && !available_space.height.is_definite());
}

void BlockFormattingContext::determine_width_of_child(Box const& box, AvailableSpace const& available_space)
{
    compute_width(box, available_space);
}

void BlockFormattingContext::determine_height_of_child(Box const& box, AvailableSpace const& available_space)
{
    compute_height(box, available_space);
}

}
