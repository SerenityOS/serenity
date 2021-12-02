/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InlineFormattingContext.h"
#include <AK/Function.h>
#include <AK/StdLibExtras.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FlexFormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

static float get_pixel_size(Box const& box, CSS::Length const& length)
{
    return length.resolved(CSS::Length::make_px(0), box, box.containing_block()->width()).to_px(box);
}

FlexFormattingContext::FlexFormattingContext(Box& flex_container, FormattingContext* parent)
    : FormattingContext(Type::Flex, flex_container, parent)
    , m_flex_direction(flex_container.computed_values().flex_direction())
{
}

FlexFormattingContext::~FlexFormattingContext()
{
}

void FlexFormattingContext::run(Box& run_box, LayoutMode)
{
    VERIFY(&run_box == &flex_container());

    // This implements https://www.w3.org/TR/css-flexbox-1/#layout-algorithm

    // FIXME: Implement reverse and ordering.

    // 1. Generate anonymous flex items
    generate_anonymous_flex_items();

    // 2. Determine the available main and cross space for the flex items
    float main_max_size = NumericLimits<float>::max();
    float main_min_size = 0;
    float cross_max_size = NumericLimits<float>::max();
    float cross_min_size = 0;
    bool main_is_constrained = false;
    bool cross_is_constrained = false;
    bool main_size_is_infinite = false;
    auto available_space = determine_available_main_and_cross_space(main_size_is_infinite, main_is_constrained, cross_is_constrained, main_min_size, main_max_size, cross_min_size, cross_max_size);
    auto main_available_size = available_space.main;
    [[maybe_unused]] auto cross_available_size = available_space.cross;

    // 3. Determine the flex base size and hypothetical main size of each item
    for (auto& flex_item : m_flex_items) {
        determine_flex_base_size_and_hypothetical_main_size(flex_item);
    }

    // 4. Determine the main size of the flex container
    determine_main_size_of_flex_container(main_is_constrained, main_size_is_infinite, main_available_size, main_min_size, main_max_size);

    // 5. Collect flex items into flex lines:
    // After this step no additional items are to be added to flex_lines or any of its items!
    collect_flex_items_into_flex_lines(main_available_size);

    // 6. Resolve the flexible lengths
    resolve_flexible_lengths(main_available_size);

    // Cross Size Determination
    // 7. Determine the hypothetical cross size of each item
    for (auto& flex_item : m_flex_items) {
        flex_item.hypothetical_cross_size = determine_hypothetical_cross_size_of_item(flex_item.box);
    }

    // 8. Calculate the cross size of each flex line.
    calculate_cross_size_of_each_flex_line(cross_min_size, cross_max_size);

    // 9. Handle 'align-content: stretch'.
    // FIXME: This

    // 10. Collapse visibility:collapse items.
    // FIXME: This

    // 11. Determine the used cross size of each flex item.
    determine_used_cross_size_of_each_flex_item();

    // 12. Distribute any remaining free space.
    distribute_any_remaining_free_space(main_available_size);

    // 13. Resolve cross-axis auto margins.
    // FIXME: This

    // 14. Align all flex items along the cross-axis
    align_all_flex_items_along_the_cross_axis();

    // 15. Determine the flex container’s used cross size:
    determine_flex_container_used_cross_size(cross_min_size, cross_max_size);

    // 16. Align all flex lines (per align-content)
    align_all_flex_lines();
}

void FlexFormattingContext::populate_specified_margins(FlexItem& item, CSS::FlexDirection flex_direction) const
{
    auto width_of_containing_block = item.box.containing_block()->width();
    // FIXME: This should also take reverse-ness into account
    if (flex_direction == CSS::FlexDirection::Row || flex_direction == CSS::FlexDirection::RowReverse) {
        item.margins.main_before = item.box.computed_values().margin().left.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
        item.margins.main_after = item.box.computed_values().margin().right.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
        item.margins.cross_before = item.box.computed_values().margin().top.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
        item.margins.cross_after = item.box.computed_values().margin().bottom.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
    } else {
        item.margins.main_before = item.box.computed_values().margin().top.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
        item.margins.main_after = item.box.computed_values().margin().bottom.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
        item.margins.cross_before = item.box.computed_values().margin().left.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
        item.margins.cross_after = item.box.computed_values().margin().right.resolved_or_zero(item.box, width_of_containing_block).to_px(item.box);
    }
};

// https://www.w3.org/TR/css-flexbox-1/#flex-items
void FlexFormattingContext::generate_anonymous_flex_items()
{
    // More like, sift through the already generated items.
    // After this step no items are to be added or removed from flex_items!
    // It holds every item we need to consider and there should be nothing in the following
    // calculations that could change that.
    // This is particularly important since we take references to the items stored in flex_items
    // later, whose addresses won't be stable if we added or removed any items.
    if (!flex_container().has_definite_width()) {
        flex_container().set_width(flex_container().containing_block()->width());
    } else {
        flex_container().set_width(flex_container().computed_values().width().resolved_or_zero(flex_container(), flex_container().containing_block()->width()).to_px(flex_container()));
    }

    if (!flex_container().has_definite_height()) {
        flex_container().set_height(flex_container().containing_block()->height());
    } else {
        flex_container().set_height(flex_container().computed_values().height().resolved_or_zero(flex_container(), flex_container().containing_block()->height()).to_px(flex_container()));
    }

    flex_container().for_each_child_of_type<Box>([&](Box& child_box) {
        (void)layout_inside(child_box, LayoutMode::Default);
        // Skip anonymous text runs that are only whitespace.
        if (child_box.is_anonymous() && !child_box.first_child_of_type<BlockContainer>()) {
            bool contains_only_white_space = true;
            child_box.for_each_in_inclusive_subtree_of_type<TextNode>([&contains_only_white_space](auto& text_node) {
                if (!text_node.text_for_rendering().is_whitespace()) {
                    contains_only_white_space = false;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
            if (contains_only_white_space)
                return IterationDecision::Continue;
        }

        // Skip any "out-of-flow" children
        if (child_box.is_out_of_flow(*this))
            return IterationDecision::Continue;

        child_box.set_flex_item(true);
        FlexItem flex_item = { child_box };
        populate_specified_margins(flex_item, m_flex_direction);
        m_flex_items.append(move(flex_item));
        return IterationDecision::Continue;
    });
}

bool FlexFormattingContext::has_definite_main_size(Box const& box) const
{
    return is_row_layout() ? box.has_definite_width() : box.has_definite_height();
}

float FlexFormattingContext::specified_main_size(Box const& box) const
{
    return is_row_layout() ? box.width() : box.height();
}

float FlexFormattingContext::specified_cross_size(Box const& box) const
{
    return is_row_layout() ? box.height() : box.width();
}

bool FlexFormattingContext::has_main_min_size(Box const& box) const
{
    auto value = is_row_layout() ? box.computed_values().min_width() : box.computed_values().min_height();
    return !value.is_undefined_or_auto();
}

bool FlexFormattingContext::has_cross_min_size(Box const& box) const
{
    auto value = is_row_layout() ? box.computed_values().min_height() : box.computed_values().min_width();
    return !value.is_undefined_or_auto();
}

bool FlexFormattingContext::has_definite_cross_size(Box const& box) const
{
    return (is_row_layout() ? box.has_definite_height() : box.has_definite_width()) && cross_size_is_absolute_or_resolved_nicely(box);
}

bool FlexFormattingContext::cross_size_is_absolute_or_resolved_nicely(NodeWithStyle const& box) const
{
    auto length = is_row_layout() ? box.computed_values().height() : box.computed_values().width();

    if (length.is_absolute() || length.is_relative())
        return true;
    if (length.is_undefined_or_auto())
        return false;

    if (!box.parent())
        return false;
    if (length.is_percentage() && cross_size_is_absolute_or_resolved_nicely(*box.parent()))
        return true;
    return false;
}

float FlexFormattingContext::specified_main_size_of_child_box(Box const& child_box) const
{
    auto main_size_of_parent = specified_main_size(flex_container());
    auto value = is_row_layout() ? child_box.computed_values().width() : child_box.computed_values().height();
    return value.resolved_or_zero(child_box, main_size_of_parent).to_px(child_box);
}

float FlexFormattingContext::specified_main_min_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_size(box, box.computed_values().min_width())
        : get_pixel_size(box, box.computed_values().min_height());
}

float FlexFormattingContext::specified_cross_min_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_size(box, box.computed_values().min_height())
        : get_pixel_size(box, box.computed_values().min_width());
}

bool FlexFormattingContext::has_main_max_size(Box const& box) const
{
    return is_row_layout()
        ? !box.computed_values().max_width().is_undefined_or_auto()
        : !box.computed_values().max_height().is_undefined_or_auto();
}

bool FlexFormattingContext::has_cross_max_size(Box const& box) const
{
    return is_row_layout()
        ? !box.computed_values().max_height().is_undefined_or_auto()
        : !box.computed_values().max_width().is_undefined_or_auto();
}

float FlexFormattingContext::specified_main_max_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_size(box, box.computed_values().max_width())
        : get_pixel_size(box, box.computed_values().max_height());
}

float FlexFormattingContext::specified_cross_max_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_size(box, box.computed_values().max_height())
        : get_pixel_size(box, box.computed_values().max_width());
}

float FlexFormattingContext::calculated_main_size(Box const& box) const
{
    return is_row_layout() ? box.width() : box.height();
}

bool FlexFormattingContext::is_cross_auto(Box const& box) const
{
    return is_row_layout() ? box.computed_values().height().is_auto() : box.computed_values().width().is_auto();
}

bool FlexFormattingContext::is_main_axis_margin_first_auto(Box const& box) const
{
    return is_row_layout() ? box.computed_values().margin().left.is_auto() : box.computed_values().margin().top.is_auto();
}

bool FlexFormattingContext::is_main_axis_margin_second_auto(Box const& box) const
{
    return is_row_layout() ? box.computed_values().margin().right.is_auto() : box.computed_values().margin().bottom.is_auto();
}

void FlexFormattingContext::set_main_size(Box& box, float size)
{
    if (is_row_layout())
        box.set_width(size);
    else
        box.set_height(size);
}

void FlexFormattingContext::set_cross_size(Box& box, float size)
{
    if (is_row_layout())
        box.set_height(size);
    else
        box.set_width(size);
}

void FlexFormattingContext::set_offset(Box& box, float main_offset, float cross_offset)
{
    if (is_row_layout())
        box.set_offset(main_offset, cross_offset);
    else
        box.set_offset(cross_offset, main_offset);
}

void FlexFormattingContext::set_main_axis_first_margin(Box& box, float margin)
{
    if (is_row_layout())
        box.box_model().margin.left = margin;
    else
        box.box_model().margin.top = margin;
}

void FlexFormattingContext::set_main_axis_second_margin(Box& box, float margin)
{
    if (is_row_layout())
        box.box_model().margin.right = margin;
    else
        box.box_model().margin.bottom = margin;
}

float FlexFormattingContext::sum_of_margin_padding_border_in_main_axis(Box const& box) const
{
    auto& margin = box.box_model().margin;
    auto& padding = box.box_model().padding;
    auto& border = box.box_model().border;

    if (is_row_layout()) {
        return margin.left + margin.right
            + padding.left + padding.right
            + border.left + border.right;
    } else {
        return margin.top + margin.bottom
            + padding.top + padding.bottom
            + border.top + border.bottom;
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-available
FlexFormattingContext::AvailableSpace FlexFormattingContext::determine_available_main_and_cross_space(bool& main_size_is_infinite, bool& main_is_constrained, bool& cross_is_constrained, float& main_min_size, float& main_max_size, float& cross_min_size, float& cross_max_size) const
{
    auto containing_block_effective_main_size = [&](Box const& box) {
        if (is_row_layout()) {
            if (box.containing_block()->has_definite_width())
                return box.containing_block()->width();
            main_size_is_infinite = true;
            return NumericLimits<float>::max();
        } else {
            if (box.containing_block()->has_definite_height())
                return box.containing_block()->height();
            main_size_is_infinite = true;
            return NumericLimits<float>::max();
        }
    };

    float main_available_space = 0;
    main_is_constrained = false;

    // For each dimension,
    //     if that dimension of the flex container’s content box is a definite size, use that;
    //     if that dimension of the flex container is being sized under a min or max-content constraint, the available space in that dimension is that constraint;
    //     otherwise, subtract the flex container’s margin, border, and padding from the space available to the flex container in that dimension and use that value. (This might result in an infinite value.)

    if (has_definite_main_size(flex_container())) {
        main_is_constrained = true;
        main_available_space = specified_main_size(flex_container());
    } else {
        if (has_main_max_size(flex_container())) {
            main_max_size = specified_main_max_size(flex_container());
            main_available_space = main_max_size;
            main_is_constrained = true;
        }
        if (has_main_min_size(flex_container())) {
            main_min_size = specified_main_min_size(flex_container());
            main_is_constrained = true;
        }

        if (!main_is_constrained) {
            auto available_main_size = containing_block_effective_main_size(flex_container());
            main_available_space = available_main_size - sum_of_margin_padding_border_in_main_axis(flex_container());
            if (flex_container().computed_values().flex_wrap() == CSS::FlexWrap::Wrap || flex_container().computed_values().flex_wrap() == CSS::FlexWrap::WrapReverse) {
                main_available_space = specified_main_size(*flex_container().containing_block());
                main_is_constrained = true;
            }
        }
    }

    float cross_available_space = 0;
    cross_is_constrained = false;

    if (has_definite_cross_size(flex_container())) {
        cross_available_space = specified_cross_size(flex_container());
    } else {
        if (has_cross_max_size(flex_container())) {
            cross_max_size = specified_cross_max_size(flex_container());
            cross_is_constrained = true;
        }
        if (has_cross_min_size(flex_container())) {
            cross_min_size = specified_cross_min_size(flex_container());
            cross_is_constrained = true;
        }

        // FIXME: Is this right? Probably not.
        if (!cross_is_constrained)
            cross_available_space = cross_max_size;
    }

    return AvailableSpace { .main = main_available_space, .cross = cross_available_space };
}

float FlexFormattingContext::layout_for_maximum_main_size(Box& box)
{
    bool main_constrained = false;
    if (is_row_layout()) {
        if (!box.computed_values().width().is_undefined_or_auto() || !box.computed_values().min_width().is_undefined_or_auto()) {
            main_constrained = true;
        }
    } else {
        if (!box.computed_values().height().is_undefined_or_auto() || !box.computed_values().min_height().is_undefined_or_auto()) {
            main_constrained = true;
        }
    }

    if (!main_constrained && box.children_are_inline()) {
        auto& block_container = verify_cast<BlockContainer>(box);
        BlockFormattingContext bfc(block_container, this);
        bfc.run(box, LayoutMode::Default);
        InlineFormattingContext ifc(block_container, &bfc);

        if (is_row_layout()) {
            ifc.run(box, LayoutMode::OnlyRequiredLineBreaks);
            return box.width();
        } else {
            ifc.run(box, LayoutMode::AllPossibleLineBreaks);
            return box.height();
        }
    }
    if (is_row_layout()) {
        (void)layout_inside(box, LayoutMode::OnlyRequiredLineBreaks);
        return box.width();
    } else {
        return BlockFormattingContext::compute_theoretical_height(box);
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-main-item
void FlexFormattingContext::determine_flex_base_size_and_hypothetical_main_size(FlexItem& flex_item)
{
    auto& child_box = flex_item.box;

    flex_item.flex_base_size = [&] {
        auto const& used_flex_basis = child_box.computed_values().flex_basis();

        // A. If the item has a definite used flex basis, that’s the flex base size.
        if (used_flex_basis.is_definite()) {
            auto specified_base_size = get_pixel_size(child_box, used_flex_basis.length);
            if (specified_base_size == 0)
                return calculated_main_size(flex_item.box);
            return specified_base_size;
        }

        // B. If the flex item has ...
        //    - an intrinsic aspect ratio,
        //    - a used flex basis of content, and
        //    - a definite cross size,
        if (flex_item.box.has_intrinsic_aspect_ratio()
            && used_flex_basis.type == CSS::FlexBasis::Content
            && has_definite_cross_size(child_box)) {
            TODO();
            // flex_base_size is calculated from definite cross size and intrinsic aspect ratio
        }

        // C. If the used flex basis is content or depends on its available space,
        //    and the flex container is being sized under a min-content or max-content constraint
        //    (e.g. when performing automatic table layout [CSS21]), size the item under that constraint.
        //    The flex base size is the item’s resulting main size.
        if (used_flex_basis.type == CSS::FlexBasis::Content
            // FIXME: && sized under min-content or max-content constraints
            && false) {
            TODO();
            // Size child_box under the constraints, flex_base_size is then the resulting main_size.
        }

        // D. Otherwise, if the used flex basis is content or depends on its available space,
        //    the available main size is infinite, and the flex item’s inline axis is parallel to the main axis,
        //    lay the item out using the rules for a box in an orthogonal flow [CSS3-WRITING-MODES].
        //    The flex base size is the item’s max-content main size.
        if (used_flex_basis.type == CSS::FlexBasis::Content
            // FIXME: && main_size is infinite && inline axis is parallel to the main axis
            && false && false) {
            TODO();
            // Use rules for a flex_container in orthogonal flow
        }

        // E. Otherwise, size the item into the available space using its used flex basis in place of its main size,
        //    treating a value of content as max-content. If a cross size is needed to determine the main size
        //    (e.g. when the flex item’s main size is in its block axis) and the flex item’s cross size is auto and not definite,
        //    in this calculation use fit-content as the flex item’s cross size.
        //    The flex base size is the item’s resulting main size.
        // FIXME: This is probably too naive.
        // FIXME: Care about FlexBasis::Auto
        if (has_definite_main_size(child_box))
            return specified_main_size_of_child_box(child_box);
        return layout_for_maximum_main_size(child_box);
    }();

    // The hypothetical main size is the item’s flex base size clamped according to its used min and max main sizes (and flooring the content box size at zero).
    auto clamp_min = has_main_min_size(child_box) ? specified_main_min_size(child_box) : 0;
    auto clamp_max = has_main_max_size(child_box) ? specified_main_max_size(child_box) : NumericLimits<float>::max();
    flex_item.hypothetical_main_size = clamp(flex_item.flex_base_size, clamp_min, clamp_max);
}

// https://www.w3.org/TR/css-flexbox-1/#algo-main-container
void FlexFormattingContext::determine_main_size_of_flex_container(bool const main_is_constrained, bool const main_size_is_infinite, float& main_available_size, float const main_min_size, float const main_max_size)
{
    if ((!main_is_constrained && main_size_is_infinite) || main_available_size == 0) {
        // Uses https://www.w3.org/TR/css-flexbox-1/#intrinsic-main-sizes
        // 9.9.1
        // 1.
        float largest_max_content_flex_fraction = 0;
        for (auto& flex_item : m_flex_items) {
            // FIXME: This needs some serious work.
            float max_content_contribution = calculated_main_size(flex_item.box);
            float max_content_flex_fraction = max_content_contribution - flex_item.flex_base_size;
            if (max_content_flex_fraction > 0) {
                max_content_flex_fraction /= max(flex_item.box.computed_values().flex_grow(), 1.0f);
            } else {
                max_content_flex_fraction /= max(flex_item.box.computed_values().flex_shrink(), 1.0f) * flex_item.flex_base_size;
            }
            flex_item.max_content_flex_fraction = max_content_flex_fraction;

            if (max_content_flex_fraction > largest_max_content_flex_fraction)
                largest_max_content_flex_fraction = max_content_flex_fraction;
        }

        // 2. Omitted
        // 3.
        float result = 0;
        for (auto& flex_item : m_flex_items) {
            auto product = 0;
            if (flex_item.max_content_flex_fraction > 0) {
                product = largest_max_content_flex_fraction * flex_item.box.computed_values().flex_grow();
            } else {
                product = largest_max_content_flex_fraction * max(flex_item.box.computed_values().flex_shrink(), 1.0f) * flex_item.flex_base_size;
            }
            result += flex_item.flex_base_size + product;
        }
        main_available_size = clamp(result, main_min_size, main_max_size);
    }
    set_main_size(flex_container(), main_available_size);
}

// https://www.w3.org/TR/css-flexbox-1/#algo-line-break
void FlexFormattingContext::collect_flex_items_into_flex_lines(float const main_available_size)
{
    // FIXME: Also support wrap-reverse

    // If the flex container is single-line, collect all the flex items into a single flex line.
    if (is_single_line()) {
        FlexLine line;
        for (auto& flex_item : m_flex_items) {
            line.items.append(&flex_item);
        }
        m_flex_lines.append(move(line));
        return;
    }

    // Otherwise, starting from the first uncollected item, collect consecutive items one by one
    // until the first time that the next collected item would not fit into the flex container’s inner main size
    // (or until a forced break is encountered, see §10 Fragmenting Flex Layout).
    // If the very first uncollected item wouldn't fit, collect just it into the line.

    // For this step, the size of a flex item is its outer hypothetical main size. (Note: This can be negative.)

    // Repeat until all flex items have been collected into flex lines.

    FlexLine line;
    float line_main_size = 0;
    for (auto& flex_item : m_flex_items) {
        if ((line_main_size + flex_item.hypothetical_main_size) > main_available_size) {
            m_flex_lines.append(move(line));
            line = {};
            line_main_size = 0;
        }
        line.items.append(&flex_item);
        line_main_size += flex_item.hypothetical_main_size;
    }
    m_flex_lines.append(move(line));
}

// https://www.w3.org/TR/css-flexbox-1/#resolve-flexible-lengths
void FlexFormattingContext::resolve_flexible_lengths(float const main_available_size)
{
    enum FlexFactor {
        FlexGrowFactor,
        FlexShrinkFactor
    };

    FlexFactor used_flex_factor;
    // 6.1. Determine used flex factor
    for (auto& flex_line : m_flex_lines) {
        size_t number_of_unfrozen_items_on_line = flex_line.items.size();

        float sum_of_hypothetical_main_sizes = 0;
        for (auto& flex_item : flex_line.items) {
            sum_of_hypothetical_main_sizes += flex_item->hypothetical_main_size;
        }
        if (sum_of_hypothetical_main_sizes < main_available_size)
            used_flex_factor = FlexFactor::FlexGrowFactor;
        else
            used_flex_factor = FlexFactor::FlexShrinkFactor;

        for (auto& flex_item : flex_line.items) {
            if (used_flex_factor == FlexFactor::FlexGrowFactor)
                flex_item->flex_factor = flex_item->box.computed_values().flex_grow();
            else if (used_flex_factor == FlexFactor::FlexShrinkFactor)
                flex_item->flex_factor = flex_item->box.computed_values().flex_shrink();
        }

        // 6.2. Size inflexible items
        auto freeze_item_setting_target_main_size_to_hypothetical_main_size = [&number_of_unfrozen_items_on_line](FlexItem& item) {
            item.target_main_size = item.hypothetical_main_size;
            number_of_unfrozen_items_on_line--;
            item.frozen = true;
        };
        for (auto& flex_item : flex_line.items) {
            if (flex_item->flex_factor.has_value() && flex_item->flex_factor.value() == 0) {
                freeze_item_setting_target_main_size_to_hypothetical_main_size(*flex_item);
            } else if (used_flex_factor == FlexFactor::FlexGrowFactor) {
                // FIXME: Spec doesn't include the == case, but we take a too basic approach to calculating the values used so this is appropriate
                if (flex_item->flex_base_size > flex_item->hypothetical_main_size) {
                    freeze_item_setting_target_main_size_to_hypothetical_main_size(*flex_item);
                }
            } else if (used_flex_factor == FlexFactor::FlexShrinkFactor) {
                if (flex_item->flex_base_size < flex_item->hypothetical_main_size) {
                    freeze_item_setting_target_main_size_to_hypothetical_main_size(*flex_item);
                }
            }
        }

        // 6.3. Calculate initial free space
        auto calculate_free_space = [&]() {
            float sum_of_items_on_line = 0;
            for (auto& flex_item : flex_line.items) {
                if (flex_item->frozen)
                    sum_of_items_on_line += flex_item->target_main_size;
                else
                    sum_of_items_on_line += flex_item->flex_base_size;
            }
            return main_available_size - sum_of_items_on_line;
        };

        float initial_free_space = calculate_free_space();

        // 6.4 Loop
        auto for_each_unfrozen_item = [&flex_line](auto callback) {
            for (auto& flex_item : flex_line.items) {
                if (!flex_item->frozen)
                    callback(flex_item);
            }
        };

        while (number_of_unfrozen_items_on_line > 0) {
            // b Calculate the remaining free space
            auto remaining_free_space = calculate_free_space();
            float sum_of_unfrozen_flex_items_flex_factors = 0;
            for_each_unfrozen_item([&](FlexItem* item) {
                sum_of_unfrozen_flex_items_flex_factors += item->flex_factor.value_or(1);
            });

            if (sum_of_unfrozen_flex_items_flex_factors < 1) {
                auto intermediate_free_space = initial_free_space * sum_of_unfrozen_flex_items_flex_factors;
                if (AK::abs(intermediate_free_space) < AK::abs(remaining_free_space))
                    remaining_free_space = intermediate_free_space;
            }

            // c Distribute free space proportional to the flex factors
            if (remaining_free_space != 0) {
                if (used_flex_factor == FlexFactor::FlexGrowFactor) {
                    float sum_of_flex_grow_factor_of_unfrozen_items = sum_of_unfrozen_flex_items_flex_factors;
                    for_each_unfrozen_item([&](FlexItem* flex_item) {
                        float ratio = flex_item->flex_factor.value_or(1) / sum_of_flex_grow_factor_of_unfrozen_items;
                        flex_item->target_main_size = flex_item->flex_base_size + (remaining_free_space * ratio);
                    });
                } else if (used_flex_factor == FlexFactor::FlexShrinkFactor) {
                    float sum_of_scaled_flex_shrink_factor_of_unfrozen_items = 0;
                    for_each_unfrozen_item([&](FlexItem* flex_item) {
                        flex_item->scaled_flex_shrink_factor = flex_item->flex_factor.value_or(1) * flex_item->flex_base_size;
                        sum_of_scaled_flex_shrink_factor_of_unfrozen_items += flex_item->scaled_flex_shrink_factor;
                    });

                    for_each_unfrozen_item([&](FlexItem* flex_item) {
                        float ratio = 1.0f;
                        if (sum_of_scaled_flex_shrink_factor_of_unfrozen_items != 0.0f)
                            ratio = flex_item->scaled_flex_shrink_factor / sum_of_scaled_flex_shrink_factor_of_unfrozen_items;
                        flex_item->target_main_size = flex_item->flex_base_size - (AK::abs(remaining_free_space) * ratio);
                    });
                }
            } else {
                // This isn't spec but makes sense.
                for_each_unfrozen_item([&](FlexItem* flex_item) {
                    flex_item->target_main_size = flex_item->flex_base_size;
                });
            }
            // d Fix min/max violations.
            float adjustments = 0.0f;
            for_each_unfrozen_item([&](FlexItem* item) {
                auto min_main = has_main_min_size(item->box)
                    ? specified_main_min_size(item->box)
                    : 0;
                auto max_main = has_main_max_size(item->box)
                    ? specified_main_max_size(item->box)
                    : NumericLimits<float>::max();

                float original_target_size = item->target_main_size;

                if (item->target_main_size < min_main) {
                    item->target_main_size = min_main;
                    item->is_min_violation = true;
                }

                if (item->target_main_size > max_main) {
                    item->target_main_size = max_main;
                    item->is_max_violation = true;
                }
                float delta = item->target_main_size - original_target_size;
                adjustments += delta;
            });
            // e Freeze over-flexed items
            float total_violation = adjustments;
            if (total_violation == 0) {
                for_each_unfrozen_item([&](FlexItem* item) {
                    --number_of_unfrozen_items_on_line;
                    item->frozen = true;
                });
            } else if (total_violation > 0) {
                for_each_unfrozen_item([&](FlexItem* item) {
                    if (item->is_min_violation) {
                        --number_of_unfrozen_items_on_line;
                        item->frozen = true;
                    }
                });
            } else if (total_violation < 0) {
                for_each_unfrozen_item([&](FlexItem* item) {
                    if (item->is_max_violation) {
                        --number_of_unfrozen_items_on_line;
                        item->frozen = true;
                    }
                });
            }
        }

        // 6.5.
        for (auto& flex_item : flex_line.items) {
            flex_item->main_size = flex_item->target_main_size;
        }
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-cross-item
float FlexFormattingContext::determine_hypothetical_cross_size_of_item(Box& box)
{
    bool cross_constrained = false;
    if (is_row_layout()) {
        if (!box.computed_values().height().is_undefined_or_auto() || !box.computed_values().min_height().is_undefined_or_auto()) {
            cross_constrained = true;
        }
    } else {
        if (!box.computed_values().width().is_undefined_or_auto() || !box.computed_values().min_width().is_undefined_or_auto()) {
            cross_constrained = true;
        }
    }

    if (!cross_constrained && box.children_are_inline()) {
        auto& block_container = verify_cast<BlockContainer>(box);
        BlockFormattingContext bfc(block_container, this);
        bfc.run(box, LayoutMode::Default);
        InlineFormattingContext ifc(block_container, &bfc);
        ifc.run(box, LayoutMode::OnlyRequiredLineBreaks);

        return is_row_layout() ? box.height() : box.width();
    }
    if (is_row_layout())
        return BlockFormattingContext::compute_theoretical_height(box);

    BlockFormattingContext context(verify_cast<BlockContainer>(box), this);
    context.compute_width(box);
    return box.width();
}

// https://www.w3.org/TR/css-flexbox-1/#algo-cross-line
void FlexFormattingContext::calculate_cross_size_of_each_flex_line(float const cross_min_size, float const cross_max_size)
{
    // If the flex container is single-line and has a definite cross size, the cross size of the flex line is the flex container’s inner cross size.
    if (is_single_line() && has_definite_cross_size(flex_container())) {
        m_flex_lines[0].cross_size = specified_cross_size(flex_container());
        return;
    }

    // Otherwise, for each flex line:
    for (auto& flex_line : m_flex_lines) {
        // FIXME: 1. Collect all the flex items whose inline-axis is parallel to the main-axis, whose align-self is baseline,
        //           and whose cross-axis margins are both non-auto. Find the largest of the distances between each item’s baseline
        //           and its hypothetical outer cross-start edge, and the largest of the distances between each item’s baseline
        //           and its hypothetical outer cross-end edge, and sum these two values.

        // FIXME: This isn't spec but makes sense here
        if (has_definite_cross_size(flex_container()) && flex_container().computed_values().align_items() == CSS::AlignItems::Stretch) {
            flex_line.cross_size = specified_cross_size(flex_container()) / m_flex_lines.size();
            continue;
        }

        // 2. Among all the items not collected by the previous step, find the largest outer hypothetical cross size.
        float largest_hypothetical_cross_size = 0;
        for (auto& flex_item : flex_line.items) {
            if (largest_hypothetical_cross_size < flex_item->hypothetical_cross_size_with_margins())
                largest_hypothetical_cross_size = flex_item->hypothetical_cross_size_with_margins();
        }

        // 3. The used cross-size of the flex line is the largest of the numbers found in the previous two steps and zero.
        flex_line.cross_size = max(0.0f, largest_hypothetical_cross_size);
    }

    // If the flex container is single-line, then clamp the line’s cross-size to be within the container’s computed min and max cross sizes.
    // Note that if CSS 2.1’s definition of min/max-width/height applied more generally, this behavior would fall out automatically.
    if (is_single_line())
        clamp(m_flex_lines[0].cross_size, cross_min_size, cross_max_size);
}

// https://www.w3.org/TR/css-flexbox-1/#algo-stretch
void FlexFormattingContext::determine_used_cross_size_of_each_flex_item()
{
    // FIXME: Get the alignment via "align-self" of the item (which accesses "align-items" of the parent if unset)
    for (auto& flex_line : m_flex_lines) {
        for (auto& flex_item : flex_line.items) {
            if (is_cross_auto(flex_item->box) && flex_container().computed_values().align_items() == CSS::AlignItems::Stretch) {
                flex_item->cross_size = flex_line.cross_size;
            } else {
                flex_item->cross_size = flex_item->hypothetical_cross_size;
            }
        }
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-main-align
void FlexFormattingContext::distribute_any_remaining_free_space(float const main_available_size)
{
    for (auto& flex_line : m_flex_lines) {
        // 12.1.
        float used_main_space = 0;
        size_t auto_margins = 0;
        for (auto& flex_item : flex_line.items) {
            used_main_space += flex_item->main_size;
            if (is_main_axis_margin_first_auto(flex_item->box))
                ++auto_margins;
            if (is_main_axis_margin_second_auto(flex_item->box))
                ++auto_margins;
        }
        float remaining_free_space = main_available_size - used_main_space;
        if (remaining_free_space > 0) {
            float size_per_auto_margin = remaining_free_space / (float)auto_margins;
            for (auto& flex_item : flex_line.items) {
                if (is_main_axis_margin_first_auto(flex_item->box))
                    set_main_axis_first_margin(flex_item->box, size_per_auto_margin);
                if (is_main_axis_margin_second_auto(flex_item->box))
                    set_main_axis_second_margin(flex_item->box, size_per_auto_margin);
            }
        } else {
            for (auto& flex_item : flex_line.items) {
                if (is_main_axis_margin_first_auto(flex_item->box))
                    set_main_axis_first_margin(flex_item->box, 0);
                if (is_main_axis_margin_second_auto(flex_item->box))
                    set_main_axis_second_margin(flex_item->box, 0);
            }
        }

        // 12.2.
        float space_between_items = 0;
        float space_before_first_item = 0;
        auto number_of_items = flex_line.items.size();

        switch (flex_container().computed_values().justify_content()) {
        case CSS::JustifyContent::FlexStart:
            break;
        case CSS::JustifyContent::FlexEnd:
            space_before_first_item = main_available_size - used_main_space;
            break;
        case CSS::JustifyContent::Center:
            space_before_first_item = (main_available_size - used_main_space) / 2.0f;
            break;
        case CSS::JustifyContent::SpaceBetween:
            space_between_items = remaining_free_space / (number_of_items - 1);
            break;
        case CSS::JustifyContent::SpaceAround:
            space_between_items = remaining_free_space / number_of_items;
            space_before_first_item = space_between_items / 2.0f;
            break;
        }

        // FIXME: Support reverse
        float main_offset = space_before_first_item;
        for (auto& flex_item : flex_line.items) {
            flex_item->main_offset = main_offset;
            main_offset += flex_item->main_size + space_between_items;
        }
    }
}

void FlexFormattingContext::align_all_flex_items_along_the_cross_axis()
{
    // FIXME: Get the alignment via "align-self" of the item (which accesses "align-items" of the parent if unset)
    // FIXME: Take better care of margins
    float line_cross_offset = 0;
    for (auto& flex_line : m_flex_lines) {
        for (auto* flex_item : flex_line.items) {
            switch (flex_container().computed_values().align_items()) {
            case CSS::AlignItems::Baseline:
                // FIXME: Implement this
                //  Fallthrough
            case CSS::AlignItems::FlexStart:
            case CSS::AlignItems::Stretch:
                flex_item->cross_offset = line_cross_offset + flex_item->margins.cross_before;
                break;
            case CSS::AlignItems::FlexEnd:
                flex_item->cross_offset = line_cross_offset + flex_line.cross_size - flex_item->cross_size;
                break;
            case CSS::AlignItems::Center:
                flex_item->cross_offset = line_cross_offset + (flex_line.cross_size / 2.0f) - (flex_item->cross_size / 2.0f);
                break;
            default:
                break;
            }
        }

        line_cross_offset += flex_line.cross_size;
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-cross-container
void FlexFormattingContext::determine_flex_container_used_cross_size(float const cross_min_size, float const cross_max_size)
{
    if (has_definite_cross_size(flex_container())) {
        float clamped_cross_size = clamp(specified_cross_size(flex_container()), cross_min_size, cross_max_size);
        set_cross_size(flex_container(), clamped_cross_size);
    } else {
        float sum_of_flex_lines_cross_sizes = 0;
        for (auto& flex_line : m_flex_lines) {
            sum_of_flex_lines_cross_sizes += flex_line.cross_size;
        }
        float clamped_cross_size = clamp(sum_of_flex_lines_cross_sizes, cross_min_size, cross_max_size);
        set_cross_size(flex_container(), clamped_cross_size);
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-line-align
void FlexFormattingContext::align_all_flex_lines()
{
    // FIXME: Support align-content
    // FIXME: Support reverse
    for (auto& flex_line : m_flex_lines) {
        for (auto* flex_item : flex_line.items) {
            set_main_size(flex_item->box, flex_item->main_size);
            set_cross_size(flex_item->box, flex_item->cross_size);
            set_offset(flex_item->box, flex_item->main_offset, flex_item->cross_offset);
        }
    }
}

}
