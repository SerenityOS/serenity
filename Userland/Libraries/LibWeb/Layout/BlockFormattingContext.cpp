/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/LineBuilder.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Layout/TableWrapper.h>
#include <LibWeb/Layout/Viewport.h>

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

CSSPixels BlockFormattingContext::automatic_content_width() const
{
    if (root().children_are_inline())
        return m_state.get(root()).content_width();
    return greatest_child_width(root());
}

CSSPixels BlockFormattingContext::automatic_content_height() const
{
    return compute_auto_height_for_block_formatting_context_root(root());
}

static bool margins_collapse_through(Box const& box, LayoutState& state)
{
    // FIXME: A box's own margins collapse if the 'min-height' property is zero, and it has neither top or bottom borders
    // nor top or bottom padding, and it has a 'height' of either 0 or 'auto', and it does not contain a line box, and
    // all of its in-flow children's margins (if any) collapse.
    // https://www.w3.org/TR/CSS22/box.html#collapsing-margins
    // FIXME: For the purpose of margin collapsing (CSS 2 §8.3.1 Collapsing margins), if the block axis is the
    //        ratio-dependent axis, it is not considered to have a computed block-size of auto.
    //        https://www.w3.org/TR/css-sizing-4/#aspect-ratio-margin-collapse

    if (box.computed_values().clear() != CSS::Clear::None)
        return false;

    return state.get(box).border_box_height() == 0;
}

void BlockFormattingContext::run(Box const&, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    if (is<Viewport>(root())) {
        layout_viewport(layout_mode, available_space);
        return;
    }

    if (root().children_are_inline())
        layout_inline_children(root(), layout_mode, available_space);
    else
        layout_block_level_children(root(), layout_mode, available_space);

    // Assign collapsed margin left after children layout of formatting context to the last child box
    if (m_margin_state.current_collapsed_margin() != 0) {
        for (auto* child_box = root().last_child_of_type<Box>(); child_box; child_box = child_box->previous_sibling_of_type<Box>()) {
            if (child_box->is_absolutely_positioned() || child_box->is_floating())
                continue;
            if (margins_collapse_through(*child_box, m_state))
                continue;
            m_state.get_mutable(*child_box).margin_bottom = m_margin_state.current_collapsed_margin();
            break;
        }
    }
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
        auto& cb_state = m_state.get(*box->containing_block());
        auto available_width = AvailableSize::make_definite(cb_state.content_width() + cb_state.padding_left + cb_state.padding_right);
        auto available_height = AvailableSize::make_definite(cb_state.content_height() + cb_state.padding_top + cb_state.padding_bottom);
        layout_absolutely_positioned_element(box, AvailableSpace(available_width, available_height));
    }
}

bool BlockFormattingContext::box_should_avoid_floats_because_it_establishes_fc(Box const& box)
{
    if (auto formatting_context_type = formatting_context_type_created_by_box(box); formatting_context_type.has_value()) {
        if (formatting_context_type.value() == Type::Block)
            return true;
        if (formatting_context_type.value() == Type::Flex)
            return true;
        if (formatting_context_type.value() == Type::Grid)
            return true;
    }
    return false;
}

void BlockFormattingContext::compute_width(Box const& box, AvailableSpace const& available_space, LayoutMode)
{
    if (box.is_absolutely_positioned()) {
        compute_width_for_absolutely_positioned_element(box, available_space);
        return;
    }

    auto remaining_available_space = available_space;
    if (available_space.width.is_definite() && box_should_avoid_floats_because_it_establishes_fc(box)) {
        // NOTE: Although CSS 2.2 specification says that only block formatting contexts should avoid floats,
        //       we also do this for flex and grid formatting contexts, because that how other engines behave.
        // 9.5 Floats
        // The border box of a table, a block-level replaced element, or an element in the normal flow that establishes a
        // new block formatting context (such as an element with 'overflow' other than 'visible') must not overlap the margin
        // box of any floats in the same block formatting context as the element itself. If necessary, implementations should
        // clear the said element by placing it below any preceding floats, but may place it adjacent to such floats if there is
        // sufficient space. They may even make the border box of said element narrower than defined by section 10.3.3.
        // CSS2 does not define when a UA may put said element next to the float or by how much said element may
        // become narrower.
        auto intrusion = intrusion_by_floats_into_box(box, 0);
        auto remaining_width = available_space.width.to_px_or_zero() - intrusion.left - intrusion.right;
        remaining_available_space.width = AvailableSize::make_definite(remaining_width);
    }

    if (box_is_sized_as_replaced_element(box)) {
        // FIXME: This should not be done *by* ReplacedBox
        if (is<ReplacedBox>(box)) {
            auto& replaced = verify_cast<ReplacedBox>(box);
            // FIXME: This const_cast is gross.
            const_cast<ReplacedBox&>(replaced).prepare_for_replaced_layout();
        }
        compute_width_for_block_level_replaced_element_in_normal_flow(box, remaining_available_space);
        if (box.is_floating()) {
            // 10.3.6 Floating, replaced elements:
            // https://www.w3.org/TR/CSS22/visudet.html#float-replaced-width
            return;
        }
    }

    if (box.is_floating()) {
        // 10.3.5 Floating, non-replaced elements:
        // https://www.w3.org/TR/CSS22/visudet.html#float-width
        compute_width_for_floating_box(box, available_space);
        return;
    }

    auto const& computed_values = box.computed_values();

    auto width_of_containing_block = remaining_available_space.width.to_px_or_zero();

    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    auto const padding_left = computed_values.padding().left().resolved(box, width_of_containing_block).to_px(box);
    auto const padding_right = computed_values.padding().right().resolved(box, width_of_containing_block).to_px(box);

    auto& box_state = m_state.get_mutable(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left;
    box_state.padding_right = padding_right;

    // NOTE: If we are calculating the min-content or max-content width of this box,
    //       and the width should be treated as auto, then we can simply return here,
    //       as the preferred width and min/max constraints are irrelevant for intrinsic sizing.
    if (box_state.width_constraint != SizeConstraint::None)
        return;

    auto try_compute_width = [&](auto const& a_width) {
        CSS::Length width = a_width;
        margin_left = computed_values.margin().left().resolved(box, width_of_containing_block);
        margin_right = computed_values.margin().right().resolved(box, width_of_containing_block);
        CSSPixels total_px = computed_values.border_left().width + computed_values.border_right().width;
        for (auto& value : { margin_left, CSS::Length::make_px(padding_left), width, CSS::Length::make_px(padding_right), margin_right }) {
            total_px += value.to_px(box);
        }

        if (!box.is_inline()) {
            // 10.3.3 Block-level, non-replaced elements in normal flow
            // If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
            if (!width.is_auto() && total_px > width_of_containing_block) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
            }

            // 10.3.3 cont'd.
            auto underflow_px = width_of_containing_block - total_px;
            if (available_space.width.is_intrinsic_sizing_constraint())
                underflow_px = 0;

            if (width.is_auto()) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;

                if (available_space.width.is_definite()) {
                    if (underflow_px >= 0) {
                        width = CSS::Length::make_px(underflow_px);
                    } else {
                        width = zero_value;
                        margin_right = CSS::Length::make_px(margin_right.to_px(box) + underflow_px);
                    }
                }
            } else {
                if (!margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_right = CSS::Length::make_px(margin_right.to_px(box) + underflow_px);
                } else if (!margin_left.is_auto() && margin_right.is_auto()) {
                    margin_right = CSS::Length::make_px(underflow_px);
                } else if (margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_left = CSS::Length::make_px(underflow_px);
                } else { // margin_left.is_auto() && margin_right.is_auto()
                    auto half_of_the_underflow = CSS::Length::make_px(underflow_px / 2);
                    margin_left = half_of_the_underflow;
                    margin_right = half_of_the_underflow;
                }
            }
        }

        return width;
    };

    auto input_width = [&] {
        if (box_is_sized_as_replaced_element(box)) {
            // NOTE: Replaced elements had their width calculated independently above.
            //       We use that width as the input here to ensure that margins get resolved.
            return CSS::Length::make_px(box_state.content_width());
        }
        if (is<TableWrapper>(box))
            return CSS::Length::make_px(compute_table_box_width_inside_table_wrapper(box, remaining_available_space));
        if (should_treat_width_as_auto(box, remaining_available_space))
            return CSS::Length::make_auto();
        return calculate_inner_width(box, remaining_available_space.width, computed_values.width());
    }();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(input_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    if (!should_treat_max_width_as_none(box, available_space.width)) {
        auto max_width = calculate_inner_width(box, remaining_available_space.width, computed_values.max_width());
        auto used_width_px = used_width.is_auto() ? CSSPixels { 0 } : used_width.to_px(box);
        if (used_width_px > max_width.to_px(box)) {
            used_width = try_compute_width(max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    if (!computed_values.min_width().is_auto()) {
        auto min_width = calculate_inner_width(box, remaining_available_space.width, computed_values.min_width());
        auto used_width_px = used_width.is_auto() ? remaining_available_space.width : AvailableSize::make_definite(used_width.to_px(box));
        if (used_width_px < min_width.to_px(box)) {
            used_width = try_compute_width(min_width);
        }
    }

    if (!box_is_sized_as_replaced_element(box) && !used_width.is_auto())
        box_state.set_content_width(used_width.to_px(box));

    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_floating_box(Box const& box, AvailableSpace const& available_space)
{
    // 10.3.5 Floating, non-replaced elements
    auto& computed_values = box.computed_values();

    auto zero_value = CSS::Length::make_px(0);
    auto width_of_containing_block = available_space.width.to_px_or_zero();

    auto margin_left = computed_values.margin().left().resolved(box, width_of_containing_block);
    auto margin_right = computed_values.margin().right().resolved(box, width_of_containing_block);
    auto const padding_left = computed_values.padding().left().resolved(box, width_of_containing_block).to_px(box);
    auto const padding_right = computed_values.padding().right().resolved(box, width_of_containing_block).to_px(box);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto compute_width = [&](auto width) {
        // If 'width' is computed as 'auto', the used value is the "shrink-to-fit" width.
        if (width.is_auto()) {
            auto result = calculate_shrink_to_fit_widths(box);

            if (available_space.width.is_definite()) {
                // Find the available width: in this case, this is the width of the containing
                // block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
                // 'padding-right', 'border-right-width', 'margin-right', and the widths of any relevant scroll bars.
                auto available_width = available_space.width.to_px_or_zero()
                    - margin_left.to_px(box) - computed_values.border_left().width - padding_left
                    - padding_right - computed_values.border_right().width - margin_right.to_px(box);
                // Then the shrink-to-fit width is: min(max(preferred minimum width, available width), preferred width).
                width = CSS::Length::make_px(min(max(result.preferred_minimum_width, available_width), result.preferred_width));
            } else if (available_space.width.is_indefinite() || available_space.width.is_max_content()) {
                // Fold the formula for shrink-to-fit width for indefinite and max-content available width.
                width = CSS::Length::make_px(result.preferred_width);
            } else {
                // Fold the formula for shrink-to-fit width for min-content available width.
                width = CSS::Length::make_px(min(result.preferred_minimum_width, result.preferred_width));
            }
        }

        return width;
    };

    auto input_width = [&] {
        if (should_treat_width_as_auto(box, available_space))
            return CSS::Length::make_auto();
        return calculate_inner_width(box, available_space.width, computed_values.width());
    }();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto width = compute_width(input_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    if (!should_treat_max_width_as_none(box, available_space.width)) {
        auto max_width = calculate_inner_width(box, available_space.width, computed_values.max_width());
        if (width.to_px(box) > max_width.to_px(box))
            width = compute_width(max_width);
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    if (!computed_values.min_width().is_auto()) {
        auto min_width = calculate_inner_width(box, available_space.width, computed_values.min_width());
        if (width.to_px(box) < min_width.to_px(box))
            width = compute_width(min_width);
    }

    auto& box_state = m_state.get_mutable(box);
    box_state.set_content_width(width.to_px(box));
    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left;
    box_state.padding_right = padding_right;
}

void BlockFormattingContext::compute_width_for_block_level_replaced_element_in_normal_flow(Box const& box, AvailableSpace const& available_space)
{
    // 10.3.6 Floating, replaced elements
    auto& computed_values = box.computed_values();

    auto zero_value = CSS::Length::make_px(0);
    auto width_of_containing_block = available_space.width.to_px_or_zero();

    // 10.3.4 Block-level, replaced elements in normal flow
    // The used value of 'width' is determined as for inline replaced elements. Then the rules for
    // non-replaced block-level elements are applied to determine the margins.
    auto margin_left = computed_values.margin().left().resolved(box, width_of_containing_block);
    auto margin_right = computed_values.margin().right().resolved(box, width_of_containing_block);
    auto const padding_left = computed_values.padding().left().resolved(box, width_of_containing_block).to_px(box);
    auto const padding_right = computed_values.padding().right().resolved(box, width_of_containing_block).to_px(box);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto& box_state = m_state.get_mutable(box);
    box_state.set_content_width(compute_width_for_replaced_element(box, available_space));
    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left;
    box_state.padding_right = padding_right;
}

CSSPixels BlockFormattingContext::compute_table_box_width_inside_table_wrapper(Box const& box, AvailableSpace const& available_space)
{
    // 17.5.2
    // Table wrapper width should be equal to width of table box it contains

    auto const& computed_values = box.computed_values();

    auto width_of_containing_block = available_space.width.to_px_or_zero();

    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = computed_values.margin().left().resolved(box, width_of_containing_block);
    auto margin_right = computed_values.margin().right().resolved(box, width_of_containing_block);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    // table-wrapper can't have borders or paddings but it might have margin taken from table-root.
    auto available_width = width_of_containing_block - margin_left.to_px(box) - margin_right.to_px(box);

    LayoutState throwaway_state(&m_state);
    auto context = create_independent_formatting_context_if_needed(throwaway_state, box);
    VERIFY(context);
    context->run(box, LayoutMode::IntrinsicSizing, m_state.get(box).available_inner_space_or_constraints_from(available_space));

    Optional<Box const&> table_box;
    box.for_each_in_subtree_of_type<Box>([&](Box const& child_box) {
        if (child_box.display().is_table_inside()) {
            table_box = child_box;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    VERIFY(table_box.has_value());

    auto table_used_width = throwaway_state.get(*table_box).border_box_width();
    return available_space.width.is_definite() ? min(table_used_width, available_width) : table_used_width;
}

void BlockFormattingContext::compute_height(Box const& box, AvailableSpace const& available_space)
{
    auto const& computed_values = box.computed_values();

    // Then work out what the height is, based on box type and CSS properties.
    CSSPixels height = 0;
    if (box_is_sized_as_replaced_element(box)) {
        height = compute_height_for_replaced_element(box, available_space);
    } else {
        if (should_treat_height_as_auto(box, available_space)) {
            height = compute_auto_height_for_block_level_element(box, m_state.get(box).available_inner_space_or_constraints_from(available_space));
        } else {
            height = calculate_inner_height(box, available_space.height, computed_values.height()).to_px(box);
        }
    }

    if (!should_treat_max_height_as_none(box, available_space.height)) {
        auto max_height = calculate_inner_height(box, available_space.height, computed_values.max_height());
        if (!max_height.is_auto())
            height = min(height, max_height.to_px(box));
    }
    if (!computed_values.min_height().is_auto()) {
        height = max(height, calculate_inner_height(box, available_space.height, computed_values.min_height()).to_px(box));
    }

    if (box.document().in_quirks_mode()
        && box.dom_node()
        && box.dom_node()->is_html_html_element()
        && box.computed_values().height().is_auto()) {
        // 3.6. The html element fills the viewport quirk
        // https://quirks.spec.whatwg.org/#the-html-element-fills-the-viewport-quirk
        // FIXME: Handle vertical writing mode.

        auto& box_state = m_state.get_mutable(box);

        // 1. Let margins be sum of the used values of the margin-left and margin-right properties of element
        //    if element has a vertical writing mode, otherwise let margins be the sum of the used values of
        //    the margin-top and margin-bottom properties of element.
        auto margins = box_state.margin_top + box_state.margin_bottom;

        // 2. Let size be the size of the initial containing block in the block flow direction minus margins.
        auto size = m_state.get(*box.containing_block()).content_height() - margins;

        // 3. Return the bigger value of size and the normal border box size the element would have
        //    according to the CSS specification.
        height = max(size, height);
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

    if (!block_container_state.has_definite_width()) {
        // NOTE: min-width or max-width for boxes with inline children can only be applied after inside layout
        //       is done and width of box content is known
        auto used_width_px = context.automatic_content_width();
        // https://www.w3.org/TR/css-sizing-3/#sizing-values
        // Percentages are resolved against the width/height, as appropriate, of the box’s containing block.
        auto containing_block_width = m_state.get(*block_container.containing_block()).content_width();
        auto available_width = AvailableSize::make_definite(containing_block_width);
        if (!should_treat_max_width_as_none(block_container, available_space.width)) {
            auto max_width_px = calculate_inner_width(block_container, available_width, block_container.computed_values().max_width()).to_px(block_container);
            if (used_width_px > max_width_px)
                used_width_px = max_width_px;
        }

        auto should_treat_min_width_as_auto = [&] {
            auto const& available_width = available_space.width;
            auto const& min_width = block_container.computed_values().min_width();
            if (min_width.is_auto())
                return true;
            if (min_width.is_fit_content() && available_width.is_intrinsic_sizing_constraint())
                return true;
            if (min_width.is_max_content() && available_width.is_max_content())
                return true;
            if (min_width.is_min_content() && available_width.is_min_content())
                return true;
            return false;
        }();
        if (!should_treat_min_width_as_auto) {
            auto min_width_px = calculate_inner_width(block_container, available_width, block_container.computed_values().min_width()).to_px(block_container);
            if (used_width_px < min_width_px)
                used_width_px = min_width_px;
        }
        block_container_state.set_content_width(used_width_px);
    }
    if (!block_container_state.has_definite_height())
        block_container_state.set_content_height(context.automatic_content_height());
}

CSSPixels BlockFormattingContext::compute_auto_height_for_block_level_element(Box const& box, AvailableSpace const& available_space)
{
    if (creates_block_formatting_context(box)) {
        return compute_auto_height_for_block_formatting_context_root(box);
    }

    auto const& box_state = m_state.get(box);

    auto display = box.display();
    if (display.is_flex_inside()) {
        // https://drafts.csswg.org/css-flexbox-1/#algo-main-container
        // NOTE: The automatic block size of a block-level flex container is its max-content size.
        return calculate_max_content_height(box, available_space.width.to_px_or_zero());
    }
    if (display.is_grid_inside()) {
        // https://www.w3.org/TR/css-grid-2/#intrinsic-sizes
        // In both inline and block formatting contexts, the grid container’s auto block size is its
        // max-content size.
        return calculate_max_content_height(box, available_space.width.to_px_or_zero());
    }
    if (display.is_table_inside()) {
        return calculate_max_content_height(box, available_space.width.to_px_or_zero());
    }

    // https://www.w3.org/TR/CSS22/visudet.html#normal-block
    // 10.6.3 Block-level non-replaced elements in normal flow when 'overflow' computes to 'visible'

    // The element's height is the distance from its top content edge to the first applicable of the following:

    // 1. the bottom edge of the last line box, if the box establishes a inline formatting context with one or more lines
    if (box.children_are_inline() && !box_state.line_boxes.is_empty())
        return box_state.line_boxes.last().bottom();

    // 2. the bottom edge of the bottom (possibly collapsed) margin of its last in-flow child, if the child's bottom margin does not collapse with the element's bottom margin
    // 3. the bottom border edge of the last in-flow child whose top margin doesn't collapse with the element's bottom margin
    if (!box.children_are_inline()) {
        for (auto* child_box = box.last_child_of_type<Box>(); child_box; child_box = child_box->previous_sibling_of_type<Box>()) {
            if (child_box->is_absolutely_positioned() || child_box->is_floating())
                continue;

            // FIXME: This is hack. If the last child is a list-item marker box, we ignore it for purposes of height calculation.
            //        Perhaps markers should not be considered in-flow(?) Perhaps they should always be the first child of the list-item
            //        box instead of the last child.
            if (child_box->is_list_item_marker_box())
                continue;

            auto const& child_box_state = m_state.get(*child_box);

            if (margins_collapse_through(*child_box, m_state))
                continue;

            auto margin_bottom = m_margin_state.current_collapsed_margin();
            if (box_state.padding_bottom == 0 && box_state.border_bottom == 0) {
                m_margin_state.box_last_in_flow_child_margin_bottom_collapsed = true;
                margin_bottom = 0;
            }

            return max(CSSPixels(0), child_box_state.offset.y() + child_box_state.content_height() + child_box_state.border_box_bottom() + margin_bottom);
        }
    }

    // 4. zero, otherwise
    return 0;
}

void BlockFormattingContext::layout_block_level_box(Box const& box, BlockContainer const& block_container, LayoutMode layout_mode, CSSPixels& bottom_of_lowest_margin_box, AvailableSpace const& available_space)
{
    auto& box_state = m_state.get_mutable(box);

    if (box.is_absolutely_positioned()) {
        m_absolutely_positioned_boxes.append(box);
        return;
    }

    // NOTE: ListItemMarkerBoxes are placed by their corresponding ListItemBox.
    if (is<ListItemMarkerBox>(box))
        return;

    resolve_vertical_box_model_metrics(box);

    if (box.is_floating()) {
        auto const y = m_y_offset_of_current_block_container.value();
        auto margin_top = !m_margin_state.has_block_container_waiting_for_final_y_position() ? m_margin_state.current_collapsed_margin() : 0;
        layout_floating_box(box, block_container, layout_mode, available_space, margin_top + y);
        bottom_of_lowest_margin_box = max(bottom_of_lowest_margin_box, box_state.offset.y() + box_state.content_height() + box_state.margin_box_bottom());
        return;
    }

    m_margin_state.add_margin(box_state.margin_top);
    auto introduce_clearance = clear_floating_boxes(box, {});
    if (introduce_clearance == DidIntroduceClearance::Yes)
        m_margin_state.reset();

    auto const y = m_y_offset_of_current_block_container.value();

    auto box_is_html_element_in_quirks_mode = box.document().in_quirks_mode()
        && box.dom_node()
        && box.dom_node()->is_html_html_element()
        && box.computed_values().height().is_auto();

    // NOTE: In quirks mode, the html element's height matches the viewport so it can be treated as definite
    if (box_state.has_definite_height() || box_is_html_element_in_quirks_mode) {
        compute_height(box, available_space);
    }

    auto independent_formatting_context = create_independent_formatting_context_if_needed(m_state, box);

    m_margin_state.update_block_waiting_for_final_y_position();
    CSSPixels margin_top = m_margin_state.current_collapsed_margin();

    if (m_margin_state.has_block_container_waiting_for_final_y_position()) {
        // If first child margin top will collapse with margin-top of containing block then margin-top of child is 0
        margin_top = 0;
    }

    if (independent_formatting_context) {
        // Margins of elements that establish new formatting contexts do not collapse with their in-flow children
        m_margin_state.reset();
    }

    place_block_level_element_in_normal_flow_vertically(box, y + margin_top);

    compute_width(box, available_space, layout_mode);

    place_block_level_element_in_normal_flow_horizontally(box, available_space);

    if (box.is_replaced_box())
        compute_height(box, available_space);

    if (independent_formatting_context) {
        // This box establishes a new formatting context. Pass control to it.
        independent_formatting_context->run(box, layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
    } else {
        // This box participates in the current block container's flow.
        if (box.children_are_inline()) {
            layout_inline_children(verify_cast<BlockContainer>(box), layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
        } else {
            if (box_state.border_top > 0 || box_state.padding_top > 0) {
                // margin-top of block container can't collapse with it's children if it has non zero border or padding
                m_margin_state.reset();
            } else if (!m_margin_state.has_block_container_waiting_for_final_y_position()) {
                // margin-top of block container can be updated during children layout hence it's final y position yet to be determined
                m_margin_state.register_block_container_y_position_update_callback([&](CSSPixels margin_top) {
                    if (introduce_clearance == DidIntroduceClearance::No) {
                        place_block_level_element_in_normal_flow_vertically(box, margin_top + y);
                    }
                });
            }

            layout_block_level_children(verify_cast<BlockContainer>(box), layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
        }
    }

    // Tables already set their height during the independent formatting context run. When multi-line text cells are involved, using different
    // available space here than during the independent formatting context run can result in different line breaks and thus a different height.
    if (!box.display().is_table_inside()) {
        compute_height(box, available_space);
    }

    if (independent_formatting_context || !margins_collapse_through(box, m_state)) {
        if (!m_margin_state.box_last_in_flow_child_margin_bottom_collapsed) {
            m_margin_state.reset();
        }
        m_y_offset_of_current_block_container = box_state.offset.y() + box_state.content_height() + box_state.border_box_bottom();
    }
    m_margin_state.box_last_in_flow_child_margin_bottom_collapsed = false;

    m_margin_state.add_margin(box_state.margin_bottom);
    m_margin_state.update_block_waiting_for_final_y_position();

    compute_inset(box);

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

    CSSPixels bottom_of_lowest_margin_box = 0;

    TemporaryChange<Optional<CSSPixels>> change { m_y_offset_of_current_block_container, CSSPixels(0) };
    block_container.for_each_child_of_type<Box>([&](Box& box) {
        layout_block_level_box(box, block_container, layout_mode, bottom_of_lowest_margin_box, available_space);
        return IterationDecision::Continue;
    });

    m_margin_state.block_container_y_position_update_callback = {};

    if (layout_mode == LayoutMode::IntrinsicSizing) {
        auto& block_container_state = m_state.get_mutable(block_container);
        if (!block_container_state.has_definite_width())
            block_container_state.set_content_width(greatest_child_width(block_container));
        if (!block_container_state.has_definite_height())
            block_container_state.set_content_height(bottom_of_lowest_margin_box);
    }
}

void BlockFormattingContext::resolve_vertical_box_model_metrics(Box const& box)
{
    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();
    auto width_of_containing_block = containing_block_width_for(box);

    box_state.margin_top = computed_values.margin().top().to_px(box, width_of_containing_block);
    box_state.margin_bottom = computed_values.margin().bottom().to_px(box, width_of_containing_block);
    box_state.border_top = computed_values.border_top().width;
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.padding_top = computed_values.padding().top().to_px(box, width_of_containing_block);
    box_state.padding_bottom = computed_values.padding().bottom().to_px(box, width_of_containing_block);
}

CSSPixels BlockFormattingContext::BlockMarginState::current_collapsed_margin() const
{
    CSSPixels smallest_margin = 0;
    CSSPixels largest_margin = 0;
    size_t negative_margin_count = 0;
    for (auto margin : current_collapsible_margins) {
        if (margin < 0)
            ++negative_margin_count;
        largest_margin = max(largest_margin, margin);
        smallest_margin = min(smallest_margin, margin);
    }

    CSSPixels collapsed_margin = 0;
    if (negative_margin_count == current_collapsible_margins.size()) {
        // When all margins are negative, the size of the collapsed margin is the smallest (most negative) margin.
        collapsed_margin = smallest_margin;
    } else if (negative_margin_count > 0) {
        // When negative margins are involved, the size of the collapsed margin is the sum of the largest positive margin and the smallest (most negative) negative margin.
        collapsed_margin = largest_margin + smallest_margin;
    } else {
        // Otherwise, collapse all the adjacent margins by using only the largest one.
        collapsed_margin = largest_margin;
    }

    return collapsed_margin;
}

BlockFormattingContext::DidIntroduceClearance BlockFormattingContext::clear_floating_boxes(Node const& child_box, Optional<InlineFormattingContext&> inline_formatting_context)
{
    auto const& computed_values = child_box.computed_values();
    auto result = DidIntroduceClearance::No;

    auto clear_floating_boxes = [&](FloatSideData& float_side) {
        if (!float_side.current_boxes.is_empty()) {
            // NOTE: Floating boxes are globally relevant within this BFC, *but* their offset coordinates
            //       are relative to their containing block.
            //       This means that we have to first convert to a root-space Y coordinate before clearing,
            //       and then convert back to a local Y coordinate when assigning the cleared offset to
            //       the `child_box` layout state.

            // First, find the lowest margin box edge on this float side and calculate the Y offset just below it.
            CSSPixels clearance_y_in_root = 0;
            for (auto const& floating_box : float_side.current_boxes) {
                auto floating_box_rect_in_root = margin_box_rect_in_ancestor_coordinate_space(floating_box.box, root());
                clearance_y_in_root = max(clearance_y_in_root, floating_box_rect_in_root.bottom());
            }

            // Then, convert the clearance Y to a coordinate relative to the containing block of `child_box`.
            CSSPixels clearance_y_in_containing_block = clearance_y_in_root;
            for (auto* containing_block = child_box.containing_block(); containing_block && containing_block != &root(); containing_block = containing_block->containing_block())
                clearance_y_in_containing_block -= m_state.get(*containing_block).offset.y();

            if (inline_formatting_context.has_value()) {
                if (clearance_y_in_containing_block > inline_formatting_context->vertical_float_clearance()) {
                    result = DidIntroduceClearance::Yes;
                    inline_formatting_context->set_vertical_float_clearance(clearance_y_in_containing_block);
                }
            } else if (clearance_y_in_containing_block > m_y_offset_of_current_block_container.value()) {
                result = DidIntroduceClearance::Yes;
                m_y_offset_of_current_block_container = clearance_y_in_containing_block;
            }

            float_side.clear();
        }
    };

    // Flex-items don't float and also don't clear.
    if ((computed_values.clear() == CSS::Clear::Left || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_left_floats);
    if ((computed_values.clear() == CSS::Clear::Right || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_right_floats);

    return result;
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_vertically(Box const& child_box, CSSPixels y)
{
    auto& box_state = m_state.get_mutable(child_box);
    y += box_state.border_box_top();
    box_state.set_content_offset(CSSPixelPoint { box_state.offset.x(), y });
}

// Returns whether the given box has the given ancestor on the path to root, ignoring the anonymous blocks.
static bool box_has_ancestor_in_non_anonymous_containing_block_chain(Box const* box, Box const& ancestor, Box const& root)
{
    Box const* current_ancestor = box ? box->non_anonymous_containing_block() : &root;
    while (current_ancestor != &root) {
        if (current_ancestor == &ancestor)
            return true;
        current_ancestor = current_ancestor->non_anonymous_containing_block();
    }
    return false;
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_horizontally(Box const& child_box, AvailableSpace const& available_space)
{
    auto& box_state = m_state.get_mutable(child_box);

    CSSPixels x = 0;
    CSSPixels available_width_within_containing_block = available_space.width.to_px_or_zero();

    if ((!m_left_floats.current_boxes.is_empty() || !m_right_floats.current_boxes.is_empty())
        && creates_block_formatting_context(child_box)) {
        auto box_in_root_rect = content_box_rect_in_ancestor_coordinate_space(child_box, root());
        auto space_and_containing_margin = space_used_and_containing_margin_for_floats(box_in_root_rect.y());
        available_width_within_containing_block -= space_and_containing_margin.left_used_space + space_and_containing_margin.right_used_space;
        auto const& containing_box_state = m_state.get(*child_box.containing_block());
        if (box_has_ancestor_in_non_anonymous_containing_block_chain(space_and_containing_margin.matching_left_float_box, *child_box.non_anonymous_containing_block(), root()))
            x = space_and_containing_margin.left_used_space;
        else
            // If the floating box doesn't share a containing block with the child box, the child box margin should overlap with the width of the floating box.
            x = max(space_and_containing_margin.left_used_space - containing_box_state.margin_left, 0);
    }

    if (child_box.containing_block()->computed_values().text_align() == CSS::TextAlign::LibwebCenter) {
        x += (available_width_within_containing_block / 2) - box_state.content_width() / 2;
    } else if (child_box.containing_block()->computed_values().text_align() == CSS::TextAlign::LibwebRight) {
        // Subtracting the left margin here because left and right margins need to be swapped when aligning to the right
        x += available_width_within_containing_block - box_state.content_width() - box_state.margin_box_left();
    } else {
        x += box_state.margin_box_left();
    }

    box_state.set_content_offset({ x, box_state.offset.y() });
}

void BlockFormattingContext::layout_viewport(LayoutMode layout_mode, AvailableSpace const& available_space)
{
    // NOTE: If we are laying out a standalone SVG document, we give it some special treatment:
    //       The root <svg> container gets the same size as the viewport,
    //       and we call directly into the SVG layout code from here.
    if (root().first_child() && root().first_child()->is_svg_svg_box()) {
        auto const& svg_root = verify_cast<SVGSVGBox>(*root().first_child());
        auto content_height = m_state.get(*svg_root.containing_block()).content_height();
        m_state.get_mutable(svg_root).set_content_height(content_height);
        auto svg_formatting_context = create_independent_formatting_context_if_needed(m_state, svg_root);
        svg_formatting_context->run(svg_root, layout_mode, available_space);
    } else {
        if (root().children_are_inline())
            layout_inline_children(root(), layout_mode, available_space);
        else
            layout_block_level_children(root(), layout_mode, available_space);
    }
}

void BlockFormattingContext::layout_floating_box(Box const& box, BlockContainer const&, LayoutMode layout_mode, AvailableSpace const& available_space, CSSPixels y, LineBuilder* line_builder)
{
    VERIFY(box.is_floating());

    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();

    resolve_vertical_box_model_metrics(box);

    compute_width(box, available_space, layout_mode);
    auto independent_formatting_context = layout_inside(box, layout_mode, box_state.available_inner_space_or_constraints_from(available_space));
    compute_height(box, available_space);

    // First we place the box normally (to get the right y coordinate.)
    // If we have a LineBuilder, we're in the middle of inline layout, otherwise this is block layout.
    if (line_builder) {
        auto y = max(line_builder->y_for_float_to_be_inserted_here(box), line_builder->inline_formatting_context().vertical_float_clearance());
        box_state.set_content_y(y + box_state.margin_box_top());
    } else {
        place_block_level_element_in_normal_flow_vertically(box, y + box_state.margin_top);
        place_block_level_element_in_normal_flow_horizontally(box, available_space);
    }

    // Then we float it to the left or right.
    auto float_box = [&](FloatSide side, FloatSideData& side_data, FloatSideData& other_side_data) {
        CSSPixels offset_from_edge = 0;
        auto float_to_edge = [&] {
            if (side == FloatSide::Left)
                offset_from_edge = box_state.margin_box_left();
            else
                offset_from_edge = box_state.content_width() + box_state.margin_box_right();
        };

        auto box_in_root_rect = content_box_rect_in_ancestor_coordinate_space(box, root());
        CSSPixels y_in_root = box_in_root_rect.y();
        CSSPixels y = box_state.offset.y();

        if (side_data.current_boxes.is_empty()) {
            // This is the first floating box on this side. Go all the way to the edge.
            float_to_edge();
            side_data.y_offset = 0;
        } else {

            // NOTE: If we're in inline layout, the LineBuilder has already provided the right Y offset.
            //       In block layout, we adjust by the side's current Y offset here.
            if (!line_builder)
                y_in_root += side_data.y_offset;

            bool did_touch_preceding_float = false;
            bool did_place_next_to_preceding_float = false;

            // Walk all currently tracked floats on the side we're floating towards.
            // We're looking for the innermost preceding float that intersects vertically with `box`.
            for (auto& preceding_float : side_data.current_boxes.in_reverse()) {
                auto const preceding_float_rect = margin_box_rect_in_ancestor_coordinate_space(preceding_float.box, root());
                if (!preceding_float_rect.contains_vertically(y_in_root))
                    continue;
                // We found a preceding float that intersects vertically with the current float.
                // Now we need to find out if there's enough inline-axis space to stack them next to each other.
                auto const& preceding_float_state = m_state.get(preceding_float.box);
                CSSPixels tentative_offset_from_edge = 0;
                bool fits_next_to_preceding_float = false;
                if (side == FloatSide::Left) {
                    tentative_offset_from_edge = max(preceding_float.offset_from_edge + preceding_float_state.content_width() + preceding_float_state.margin_box_right(), 0) + box_state.margin_box_left();
                    if (available_space.width.is_definite()) {
                        fits_next_to_preceding_float = (tentative_offset_from_edge + box_state.content_width() + box_state.margin_box_right()) <= available_space.width.to_px_or_zero();
                    } else if (available_space.width.is_max_content() || available_space.width.is_indefinite()) {
                        fits_next_to_preceding_float = true;
                    }
                } else {
                    tentative_offset_from_edge = preceding_float.offset_from_edge + preceding_float_state.margin_box_left() + box_state.margin_box_right() + box_state.content_width();
                    fits_next_to_preceding_float = tentative_offset_from_edge >= 0;
                }
                did_touch_preceding_float = true;
                if (!fits_next_to_preceding_float)
                    break;
                offset_from_edge = tentative_offset_from_edge;
                did_place_next_to_preceding_float = true;
                break;
            }

            auto has_clearance = false;
            if (side == FloatSide::Left) {
                has_clearance = computed_values.clear() == CSS::Clear::Left || computed_values.clear() == CSS::Clear::Both;
            } else if (side == FloatSide::Right) {
                has_clearance = computed_values.clear() == CSS::Clear::Right || computed_values.clear() == CSS::Clear::Both;
            }

            if (!did_touch_preceding_float || !did_place_next_to_preceding_float || has_clearance) {
                // One of three things happened:
                // - This box does not touch another floating box.
                // - We ran out of horizontal space on this "float line", and need to break.
                // - This box has clearance.
                // Either way, we float this box all the way to the edge.
                float_to_edge();
                CSSPixels lowest_margin_edge = 0;
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

        auto top_margin_edge = y - box_state.margin_box_top();
        side_data.all_boxes.append(adopt_own(*new FloatingBox {
            .box = box,
            .offset_from_edge = offset_from_edge,
            .top_margin_edge = top_margin_edge,
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
        if (top_margin_edge > other_side_data.y_offset)
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

    compute_inset(box);

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void BlockFormattingContext::layout_list_item_marker(ListItemBox const& list_item_box)
{
    if (!list_item_box.marker())
        return;

    auto& marker = *list_item_box.marker();
    auto& marker_state = m_state.get_mutable(marker);
    auto& list_item_state = m_state.get_mutable(list_item_box);

    CSSPixels image_width = 0;
    CSSPixels image_height = 0;
    if (auto const* list_style_image = marker.list_style_image()) {
        image_width = list_style_image->natural_width().value_or(0);
        image_height = list_style_image->natural_height().value_or(0);
    }

    CSSPixels default_marker_width = max(4, marker.font().pixel_size_rounded_up() - 4);

    auto marker_text = marker.text().value_or("");
    if (marker_text.is_empty()) {
        marker_state.set_content_width(image_width + default_marker_width);
    } else {
        auto text_width = marker.font().width(marker_text);
        marker_state.set_content_width(image_width + CSSPixels::nearest_value_for(text_width));
    }

    marker_state.set_content_height(max(image_height, marker.font().pixel_size_rounded_up() + 1));

    if (marker.list_style_type() == CSS::ListStyleType::DisclosureClosed || marker.list_style_type() == CSS::ListStyleType::DisclosureOpen)
        marker_state.set_content_width(marker_state.content_height());

    auto final_marker_width = marker_state.content_width() + default_marker_width;

    if (marker.list_style_position() == CSS::ListStylePosition::Inside) {
        list_item_state.set_content_offset({ final_marker_width, list_item_state.offset.y() });
        list_item_state.set_content_width(list_item_state.content_width() - final_marker_width);
    }

    auto offset_y = max(CSSPixels(0), (marker.line_height() - marker_state.content_height()) / 2);
    auto space_and_containing_margin = intrusion_by_floats_into_box(list_item_box, offset_y);
    marker_state.set_content_offset({ space_and_containing_margin.left - final_marker_width, offset_y });

    if (marker_state.content_height() > list_item_state.content_height())
        list_item_state.set_content_height(marker_state.content_height());
}

BlockFormattingContext::SpaceUsedAndContainingMarginForFloats BlockFormattingContext::space_used_and_containing_margin_for_floats(CSSPixels y) const
{
    SpaceUsedAndContainingMarginForFloats space_and_containing_margin;

    for (auto const& floating_box_ptr : m_left_floats.all_boxes.in_reverse()) {
        auto const& floating_box = *floating_box_ptr;
        auto const& floating_box_state = m_state.get(floating_box.box);
        // NOTE: The floating box is *not* in the final horizontal position yet, but the size and vertical position is valid.
        auto rect = margin_box_rect_in_ancestor_coordinate_space(floating_box.box, root());
        if (rect.contains_vertically(y)) {
            CSSPixels offset_from_containing_block_chain_margins_between_here_and_root = 0;
            for (auto const* containing_block = floating_box.box->containing_block(); containing_block && containing_block != &root(); containing_block = containing_block->containing_block()) {
                auto const& containing_block_state = m_state.get(*containing_block);
                offset_from_containing_block_chain_margins_between_here_and_root += containing_block_state.margin_box_left();
            }
            space_and_containing_margin.left_used_space = floating_box.offset_from_edge
                + floating_box_state.content_width()
                + floating_box_state.margin_box_right();
            space_and_containing_margin.left_total_containing_margin = offset_from_containing_block_chain_margins_between_here_and_root;
            space_and_containing_margin.matching_left_float_box = floating_box.box.ptr();
            break;
        }
    }

    for (auto const& floating_box_ptr : m_right_floats.all_boxes.in_reverse()) {
        auto const& floating_box = *floating_box_ptr;
        auto const& floating_box_state = m_state.get(floating_box.box);
        // NOTE: The floating box is *not* in the final horizontal position yet, but the size and vertical position is valid.
        auto rect = margin_box_rect_in_ancestor_coordinate_space(floating_box.box, root());
        if (rect.contains_vertically(y)) {
            CSSPixels offset_from_containing_block_chain_margins_between_here_and_root = 0;
            for (auto const* containing_block = floating_box.box->containing_block(); containing_block && containing_block != &root(); containing_block = containing_block->containing_block()) {
                auto const& containing_block_state = m_state.get(*containing_block);
                offset_from_containing_block_chain_margins_between_here_and_root += containing_block_state.margin_box_right();
            }
            space_and_containing_margin.right_used_space = floating_box.offset_from_edge
                + floating_box_state.margin_box_left();
            space_and_containing_margin.right_total_containing_margin = offset_from_containing_block_chain_margins_between_here_and_root;
            break;
        }
    }

    return space_and_containing_margin;
}

FormattingContext::SpaceUsedByFloats BlockFormattingContext::intrusion_by_floats_into_box(Box const& box, CSSPixels y_in_box) const
{
    // NOTE: Floats are relative to the BFC root box, not necessarily the containing block of this IFC.
    auto box_in_root_rect = content_box_rect_in_ancestor_coordinate_space(box, root());
    CSSPixels y_in_root = box_in_root_rect.y() + y_in_box;
    auto space_and_containing_margin = space_used_and_containing_margin_for_floats(y_in_root);
    auto left_side_floats_limit_to_right = space_and_containing_margin.left_total_containing_margin + space_and_containing_margin.left_used_space;
    auto right_side_floats_limit_to_right = space_and_containing_margin.right_used_space + space_and_containing_margin.right_total_containing_margin;

    auto left_intrusion = max(CSSPixels(0), left_side_floats_limit_to_right - max(CSSPixels(0), box_in_root_rect.x()));

    CSSPixels offset_from_containing_block_chain_margins_between_here_and_root = 0;
    for (auto const* containing_block = static_cast<Box const*>(&box); containing_block && containing_block != &root(); containing_block = containing_block->containing_block()) {
        auto const& containing_block_state = m_state.get(*containing_block);
        offset_from_containing_block_chain_margins_between_here_and_root = max(offset_from_containing_block_chain_margins_between_here_and_root, containing_block_state.margin_box_right());
    }
    auto right_intrusion = max(CSSPixels(0), right_side_floats_limit_to_right - offset_from_containing_block_chain_margins_between_here_and_root);

    return { left_intrusion, right_intrusion };
}

CSSPixels BlockFormattingContext::greatest_child_width(Box const& box) const
{
    // Similar to FormattingContext::greatest_child_width()
    // but this one takes floats into account!
    CSSPixels max_width = m_left_floats.max_width + m_right_floats.max_width;
    if (box.children_are_inline()) {
        for (auto const& line_box : m_state.get(verify_cast<BlockContainer>(box)).line_boxes) {
            CSSPixels width_here = line_box.width();
            CSSPixels extra_width_from_left_floats = 0;
            for (auto& left_float : m_left_floats.all_boxes) {
                // NOTE: Floats directly affect the automatic size of their containing block, but only indirectly anything above in the tree.
                if (left_float->box->containing_block() != &box)
                    continue;
                if (line_box.baseline() >= left_float->top_margin_edge || line_box.baseline() <= left_float->bottom_margin_edge) {
                    auto const& left_float_state = m_state.get(left_float->box);
                    extra_width_from_left_floats = max(extra_width_from_left_floats, left_float->offset_from_edge + left_float_state.content_width() + left_float_state.margin_box_right());
                }
            }
            CSSPixels extra_width_from_right_floats = 0;
            for (auto& right_float : m_right_floats.all_boxes) {
                // NOTE: Floats directly affect the automatic size of their containing block, but only indirectly anything above in the tree.
                if (right_float->box->containing_block() != &box)
                    continue;
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
                max_width = max(max_width, m_state.get(child).margin_box_width());
        });
    }
    return max_width;
}

void BlockFormattingContext::determine_width_of_child(Box const&, AvailableSpace const&)
{
    // NOTE: We don't do anything here, since we'll have already determined the width of the child
    //       before recursing into nested layout within the child.
}

void BlockFormattingContext::determine_height_of_child(Box const& box, AvailableSpace const& available_space)
{
    compute_height(box, available_space);
}

}
