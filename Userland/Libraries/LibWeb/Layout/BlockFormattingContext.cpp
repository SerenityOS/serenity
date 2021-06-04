/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Page/BrowsingContext.h>

namespace Web::Layout {

BlockFormattingContext::BlockFormattingContext(Box& context_box, FormattingContext* parent)
    : FormattingContext(context_box, parent)
{
}

BlockFormattingContext::~BlockFormattingContext()
{
}

bool BlockFormattingContext::is_initial() const
{
    return is<InitialContainingBlockBox>(context_box());
}

void BlockFormattingContext::run(Box& box, LayoutMode layout_mode)
{
    if (is_initial()) {
        layout_initial_containing_block(layout_mode);
        return;
    }

    // FIXME: BFC currently computes the width+height of the target box.
    //        This is necessary to be able to place absolutely positioned descendants.
    //        The same work is also done by the parent BFC for each of its blocks..

    if (layout_mode == LayoutMode::Default)
        compute_width(box);

    if (box.children_are_inline()) {
        layout_inline_children(box, layout_mode);
    } else {
        layout_block_level_children(box, layout_mode);
    }

    if (layout_mode == LayoutMode::Default) {
        compute_height(box);

        box.for_each_child_of_type<Box>([&](auto& child_box) {
            if (child_box.is_absolutely_positioned()) {
                layout_absolutely_positioned_element(child_box);
            }
            return IterationDecision::Continue;
        });
    }
}

void BlockFormattingContext::compute_width(Box& box)
{
    if (box.is_absolutely_positioned()) {
        compute_width_for_absolutely_positioned_element(box);
        return;
    }

    if (is<ReplacedBox>(box)) {
        // FIXME: This should not be done *by* ReplacedBox
        auto& replaced = downcast<ReplacedBox>(box);
        replaced.prepare_for_replaced_layout();
        compute_width_for_block_level_replaced_element_in_normal_flow(replaced);
        return;
    }

    if (box.is_floating()) {
        compute_width_for_floating_box(box);
        return;
    }

    auto& computed_values = box.computed_values();
    float width_of_containing_block = box.width_of_logical_containing_block();

    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    const auto padding_left = computed_values.padding().left.resolved_or_zero(box, width_of_containing_block);
    const auto padding_right = computed_values.padding().right.resolved_or_zero(box, width_of_containing_block);

    auto try_compute_width = [&](const auto& a_width) {
        CSS::Length width = a_width;
        margin_left = computed_values.margin().left.resolved_or_zero(box, width_of_containing_block);
        margin_right = computed_values.margin().right.resolved_or_zero(box, width_of_containing_block);

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

            if (width.is_auto()) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
                if (underflow_px >= 0) {
                    width = CSS::Length(underflow_px, CSS::Length::Type::Px);
                } else {
                    width = zero_value;
                    margin_right = CSS::Length(margin_right.to_px(box) + underflow_px, CSS::Length::Type::Px);
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
        } else if (box.is_inline_block()) {

            // 10.3.9 'Inline-block', non-replaced elements in normal flow

            // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
            if (margin_left.is_auto())
                margin_left = zero_value;
            if (margin_right.is_auto())
                margin_right = zero_value;

            // If 'width' is 'auto', the used value is the shrink-to-fit width as for floating elements.
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
        }

        return width;
    };

    auto specified_width = computed_values.width().resolved_or_auto(box, width_of_containing_block);

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = computed_values.max_width().resolved_or_auto(box, width_of_containing_block);
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(box) > specified_max_width.to_px(box)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = computed_values.min_width().resolved_or_auto(box, width_of_containing_block);
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(box) < specified_min_width.to_px(box)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    box.set_width(used_width.to_px(box));
    box.box_model().margin.left = margin_left.to_px(box);
    box.box_model().margin.right = margin_right.to_px(box);
    box.box_model().border.left = computed_values.border_left().width;
    box.box_model().border.right = computed_values.border_right().width;
    box.box_model().padding.left = padding_left.to_px(box);
    box.box_model().padding.right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_floating_box(Box& box)
{
    // 10.3.5 Floating, non-replaced elements
    auto& computed_values = box.computed_values();
    float width_of_containing_block = box.width_of_logical_containing_block();
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = computed_values.margin().left.resolved_or_zero(box, width_of_containing_block);
    auto margin_right = computed_values.margin().right.resolved_or_zero(box, width_of_containing_block);
    const auto padding_left = computed_values.padding().left.resolved_or_zero(box, width_of_containing_block);
    const auto padding_right = computed_values.padding().right.resolved_or_zero(box, width_of_containing_block);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto width = computed_values.width().resolved_or_auto(box, width_of_containing_block);

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

    float final_width = width.resolved_or_zero(box, width_of_containing_block).to_px(box);
    box.set_width(final_width);
    box.box_model().margin.left = margin_left.to_px(box);
    box.box_model().margin.right = margin_right.to_px(box);
    box.box_model().border.left = computed_values.border_left().width;
    box.box_model().border.right = computed_values.border_right().width;
    box.box_model().padding.left = padding_left.to_px(box);
    box.box_model().padding.right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox& box)
{
    box.set_width(compute_width_for_replaced_element(box));
}

static float compute_auto_height_for_block_level_element(const Box& box)
{
    Optional<float> top;
    Optional<float> bottom;

    if (box.children_are_inline()) {
        // If it only has inline-level children, the height is the distance between
        // the top of the topmost line box and the bottom of the bottommost line box.
        if (!box.line_boxes().is_empty()) {
            for (auto& fragment : box.line_boxes().first().fragments()) {
                if (!top.has_value() || fragment.offset().y() < top.value())
                    top = fragment.offset().y();
            }
            for (auto& fragment : box.line_boxes().last().fragments()) {
                if (!bottom.has_value() || (fragment.offset().y() + fragment.height()) > bottom.value())
                    bottom = fragment.offset().y() + fragment.height();
            }
        }
    } else {
        // If it has block-level children, the height is the distance between
        // the top margin-edge of the topmost block-level child box
        // and the bottom margin-edge of the bottommost block-level child box.
        box.for_each_child_of_type<Box>([&](Layout::Box& child_box) {
            if (child_box.is_absolutely_positioned())
                return IterationDecision::Continue;
            if ((box.computed_values().overflow_y() == CSS::Overflow::Visible) && child_box.is_floating())
                return IterationDecision::Continue;

            float child_box_top = child_box.effective_offset().y() - child_box.box_model().margin_box().top;
            float child_box_bottom = child_box.effective_offset().y() + child_box.height() + child_box.box_model().margin_box().bottom;

            if (!top.has_value() || child_box_top < top.value())
                top = child_box_top;

            if (!bottom.has_value() || child_box_bottom > bottom.value())
                bottom = child_box_bottom;

            return IterationDecision::Continue;
        });
    }
    return bottom.value_or(0) - top.value_or(0);
}

float BlockFormattingContext::compute_theoretical_height(const Box& box)
{
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();

    // Then work out what the height is, based on box type and CSS properties.
    float height = 0;
    if (is<ReplacedBox>(box)) {
        height = compute_height_for_replaced_element(downcast<ReplacedBox>(box));
    } else {
        if (box.computed_values().height().is_undefined_or_auto()
            || (computed_values.height().is_percentage() && !containing_block.computed_values().height().is_absolute())) {
            height = compute_auto_height_for_block_level_element(box);
        } else {
            height = computed_values.height().resolved_or_auto(box, containing_block.height()).to_px(box);
        }
    }

    auto specified_max_height = computed_values.max_height().resolved_or_auto(box, containing_block.height());
    if (!specified_max_height.is_auto()
        && !(computed_values.max_height().is_percentage() && !containing_block.computed_values().height().is_absolute()))
        height = min(height, specified_max_height.to_px(box));
    auto specified_min_height = computed_values.min_height().resolved_or_auto(box, containing_block.height());
    if (!specified_min_height.is_auto()
        && !(computed_values.min_height().is_percentage() && !containing_block.computed_values().height().is_absolute()))
        height = max(height, specified_min_height.to_px(box));
    return height;
}

void BlockFormattingContext::compute_height(Box& box)
{
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();
    // First, resolve the top/bottom parts of the surrounding box model.
    box.box_model().margin.top = computed_values.margin().top.resolved_or_zero(box, containing_block.width()).to_px(box);
    box.box_model().margin.bottom = computed_values.margin().bottom.resolved_or_zero(box, containing_block.width()).to_px(box);
    box.box_model().border.top = computed_values.border_top().width;
    box.box_model().border.bottom = computed_values.border_bottom().width;
    box.box_model().padding.top = computed_values.padding().top.resolved_or_zero(box, containing_block.width()).to_px(box);
    box.box_model().padding.bottom = computed_values.padding().bottom.resolved_or_zero(box, containing_block.width()).to_px(box);

    auto height = compute_theoretical_height(box);
    box.set_height(height);
}

void BlockFormattingContext::compute_position(Box& box)
{
    // 9.4.3 Relative positioning
    // Once a box has been laid out according to the normal flow or floated, it may be shifted relative to this position.

    auto& box_model = box.box_model();
    auto& computed_values = box.computed_values();
    float width_of_containing_block = box.width_of_logical_containing_block();

    auto specified_left = computed_values.offset().left.resolved_or_zero(box, width_of_containing_block);
    auto specified_right = computed_values.offset().right.resolved_or_zero(box, width_of_containing_block);

    if (specified_left.is_auto() && specified_right.is_auto()) {
        // If both 'left' and 'right' are 'auto' (their initial values), the used values are '0' (i.e., the boxes stay in their original position).
        box_model.offset.left = 0;
        box_model.offset.right = 0;
    } else if (specified_left.is_auto()) {
        // If 'left' is 'auto', its used value is minus the value of 'right' (i.e., the boxes move to the left by the value of 'right').
        box_model.offset.right = specified_right.to_px(box);
        box_model.offset.left = 0 - box_model.offset.right;
    } else if (specified_right.is_auto()) {
        // If 'right' is specified as 'auto', its used value is minus the value of 'left'.
        box_model.offset.left = specified_left.to_px(box);
        box_model.offset.right = 0 - box_model.offset.left;
    } else {
        // If neither 'left' nor 'right' is 'auto', the position is over-constrained, and one of them has to be ignored.
        // If the 'direction' property of the containing block is 'ltr', the value of 'left' wins and 'right' becomes -'left'.
        // If 'direction' of the containing block is 'rtl', 'right' wins and 'left' is ignored.
        // FIXME: Check direction (assuming 'ltr' for now).
        box_model.offset.left = specified_left.to_px(box);
        box_model.offset.right = 0 - box_model.offset.left;
    }
}

void BlockFormattingContext::layout_inline_children(Box& box, LayoutMode layout_mode)
{
    InlineFormattingContext context(box, this);
    context.run(box, layout_mode);
}

void BlockFormattingContext::layout_block_level_children(Box& box, LayoutMode layout_mode)
{
    float content_height = 0;
    float content_width = 0;

    box.for_each_child_of_type<Box>([&](auto& child_box) {
        if (child_box.is_absolutely_positioned())
            return IterationDecision::Continue;

        if (child_box.is_floating()) {
            layout_floating_child(child_box, box);
            return IterationDecision::Continue;
        }

        compute_width(child_box);
        layout_inside(child_box, layout_mode);
        compute_height(child_box);

        if (child_box.computed_values().position() == CSS::Position::Relative)
            compute_position(child_box);

        if (is<ReplacedBox>(child_box))
            place_block_level_replaced_element_in_normal_flow(child_box, box);
        else if (is<BlockBox>(child_box))
            place_block_level_non_replaced_element_in_normal_flow(child_box, box);

        // FIXME: This should be factored differently. It's uncool that we mutate the tree *during* layout!
        //        Instead, we should generate the marker box during the tree build.
        if (is<ListItemBox>(child_box))
            downcast<ListItemBox>(child_box).layout_marker();

        content_height = max(content_height, child_box.effective_offset().y() + child_box.height() + child_box.box_model().margin_box().bottom);
        content_width = max(content_width, downcast<Box>(child_box).width());
        return IterationDecision::Continue;
    });

    if (layout_mode != LayoutMode::Default) {
        if (box.computed_values().width().is_undefined() || box.computed_values().width().is_auto())
            box.set_width(content_width);
    }
}

void BlockFormattingContext::place_block_level_replaced_element_in_normal_flow(Box& child_box, Box& containing_block)
{
    VERIFY(!containing_block.is_absolutely_positioned());
    auto& replaced_element_box_model = child_box.box_model();

    replaced_element_box_model.margin.top = child_box.computed_values().margin().top.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);
    replaced_element_box_model.margin.bottom = child_box.computed_values().margin().bottom.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);
    replaced_element_box_model.border.top = child_box.computed_values().border_top().width;
    replaced_element_box_model.border.bottom = child_box.computed_values().border_bottom().width;
    replaced_element_box_model.padding.top = child_box.computed_values().padding().top.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);
    replaced_element_box_model.padding.bottom = child_box.computed_values().padding().bottom.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);

    float x = replaced_element_box_model.margin.left
        + replaced_element_box_model.border.left
        + replaced_element_box_model.padding.left
        + replaced_element_box_model.offset.left;

    float y = replaced_element_box_model.margin_box().top + containing_block.box_model().offset.top;

    child_box.set_offset(x, y);
}

void BlockFormattingContext::place_block_level_non_replaced_element_in_normal_flow(Box& child_box, Box& containing_block)
{
    auto& box_model = child_box.box_model();
    auto& computed_values = child_box.computed_values();

    box_model.margin.top = computed_values.margin().top.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);
    box_model.margin.bottom = computed_values.margin().bottom.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);
    box_model.border.top = computed_values.border_top().width;
    box_model.border.bottom = computed_values.border_bottom().width;
    box_model.padding.top = computed_values.padding().top.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);
    box_model.padding.bottom = computed_values.padding().bottom.resolved_or_zero(containing_block, containing_block.width()).to_px(child_box);

    float x = box_model.margin.left
        + box_model.border.left
        + box_model.padding.left
        + box_model.offset.left;

    if (containing_block.computed_values().text_align() == CSS::TextAlign::LibwebCenter) {
        x = (containing_block.width() / 2) - child_box.width() / 2;
    }

    float y = box_model.margin_box().top
        + box_model.offset.top;

    // NOTE: Empty (0-height) preceding siblings have their margins collapsed with *their* preceding sibling, etc.
    float collapsed_bottom_margin_of_preceding_siblings = 0;

    auto* relevant_sibling = child_box.previous_sibling_of_type<Layout::BlockBox>();
    while (relevant_sibling != nullptr) {
        if (!relevant_sibling->is_absolutely_positioned() && !relevant_sibling->is_floating()) {
            collapsed_bottom_margin_of_preceding_siblings = max(collapsed_bottom_margin_of_preceding_siblings, relevant_sibling->box_model().margin.bottom);
            if (relevant_sibling->border_box_height() > 0)
                break;
        }
        relevant_sibling = relevant_sibling->previous_sibling();
    }

    if (relevant_sibling) {
        y += relevant_sibling->effective_offset().y()
            + relevant_sibling->height()
            + relevant_sibling->box_model().border_box().bottom;

        // Collapse top margin with bottom margin of preceding siblings if needed
        float my_margin_top = box_model.margin.top;

        if (my_margin_top < 0 || collapsed_bottom_margin_of_preceding_siblings < 0) {
            // Negative margins present.
            float largest_negative_margin = -min(my_margin_top, collapsed_bottom_margin_of_preceding_siblings);
            float largest_positive_margin = (my_margin_top < 0 && collapsed_bottom_margin_of_preceding_siblings < 0) ? 0 : max(my_margin_top, collapsed_bottom_margin_of_preceding_siblings);
            float final_margin = largest_positive_margin - largest_negative_margin;
            y += final_margin - my_margin_top;
        } else if (collapsed_bottom_margin_of_preceding_siblings > my_margin_top) {
            // Sibling's margin is larger than mine, adjust so we use sibling's.
            y += collapsed_bottom_margin_of_preceding_siblings - my_margin_top;
        }
    }

    auto clear_floating_boxes = [&](auto& floating_boxes) {
        if (!floating_boxes.is_empty()) {
            float clearance_y = 0;
            for (auto* floating_box : floating_boxes) {
                clearance_y = max(clearance_y, floating_box->effective_offset().y() + floating_box->border_box_height());
            }
            y = max(y, clearance_y);
            floating_boxes.clear();
        }
    };

    // Flex-items don't float and also don't clear.
    if ((computed_values.clear() == CSS::Clear::Left || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_left_floating_boxes);
    if ((computed_values.clear() == CSS::Clear::Right || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_right_floating_boxes);

    child_box.set_offset(x, y);
}

void BlockFormattingContext::layout_initial_containing_block(LayoutMode layout_mode)
{
    auto viewport_rect = context_box().browsing_context().viewport_rect();

    auto& icb = downcast<Layout::InitialContainingBlockBox>(context_box());
    icb.build_stacking_context_tree();

    icb.set_width(viewport_rect.width());

    layout_block_level_children(context_box(), layout_mode);

    VERIFY(!icb.children_are_inline());

    // FIXME: The ICB should have the height of the viewport.
    //        Instead of auto-sizing the ICB, we should spill into overflow.
    float lowest_bottom = 0;
    icb.for_each_child_of_type<Box>([&](auto& child) {
        lowest_bottom = max(lowest_bottom, child.absolute_rect().bottom());
    });

    // FIXME: This is a hack and should be managed by an overflow mechanism.
    icb.set_height(max(static_cast<float>(viewport_rect.height()), lowest_bottom));
}

static Gfx::FloatRect rect_in_coordinate_space(const Box& box, const Box& context_box)
{
    Gfx::FloatRect rect = box.margin_box_as_relative_rect();
    for (auto* ancestor = box.parent(); ancestor; ancestor = ancestor->parent()) {
        if (is<Box>(*ancestor)) {
            auto offset = downcast<Box>(*ancestor).effective_offset();
            rect.translate_by(offset);
        }
        if (ancestor == &context_box)
            break;
    }
    return rect;
}

void BlockFormattingContext::layout_floating_child(Box& box, Box& containing_block)
{
    VERIFY(box.is_floating());

    compute_width(box);
    layout_inside(box, LayoutMode::Default);
    compute_height(box);

    // First we place the box normally (to get the right y coordinate.)
    place_block_level_non_replaced_element_in_normal_flow(box, containing_block);

    // Then we float it to the left or right.
    float x = box.effective_offset().x();

    auto box_in_context_rect = rect_in_coordinate_space(box, context_box());
    float y_in_context_box = box_in_context_rect.y();

    // Next, float to the left and/or right
    if (box.computed_values().float_() == CSS::Float::Left) {
        if (!m_left_floating_boxes.is_empty()) {
            auto& previous_floating_box = *m_left_floating_boxes.last();
            auto previous_rect = rect_in_coordinate_space(previous_floating_box, context_box());
            if (previous_rect.contains_vertically(y_in_context_box)) {
                // This box touches another already floating box. Stack to the right.
                x = previous_floating_box.margin_box_as_relative_rect().x() + previous_floating_box.margin_box_as_relative_rect().width() + box.box_model().margin_box().left;
            } else {
                // This box does not touch another floating box, go all the way to the left.
                x = box.box_model().margin_box().left;
                // Also, forget all previous left-floating boxes while we're here since they're no longer relevant.
                m_left_floating_boxes.clear();
            }
        } else {
            // This is the first left-floating box. Go all the way to the left.
            x = box.box_model().margin_box().left;
        }
        m_left_floating_boxes.append(&box);
    } else if (box.computed_values().float_() == CSS::Float::Right) {
        if (!m_right_floating_boxes.is_empty()) {
            auto& previous_floating_box = *m_right_floating_boxes.last();
            auto previous_rect = rect_in_coordinate_space(previous_floating_box, context_box());
            if (previous_rect.contains_vertically(y_in_context_box)) {
                // This box touches another already floating box. Stack to the left.
                x = previous_floating_box.margin_box_as_relative_rect().x() - box.box_model().margin_box().right - box.width();
            } else {
                // This box does not touch another floating box, go all the way to the right.
                x = containing_block.width() - box.box_model().margin_box().right - box.width();
                // Also, forget all previous right-floating boxes while we're here since they're no longer relevant.
                m_right_floating_boxes.clear();
            }
        } else {
            // This is the first right-floating box. Go all the way to the right.
            x = containing_block.width() - box.box_model().margin_box().right - box.width();
        }
        m_right_floating_boxes.append(&box);
    }

    box.set_offset(x, box.effective_offset().y());
}

}
