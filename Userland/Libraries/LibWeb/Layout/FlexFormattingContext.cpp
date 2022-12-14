/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InlineFormattingContext.h"
#include <AK/Function.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FlexFormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

// NOTE: We use a custom clamping function here instead of AK::clamp(), since the AK version
//       will VERIFY(max >= min) and CSS explicitly allows that (see css-values-4.)
template<typename T>
[[nodiscard]] constexpr T css_clamp(T const& value, T const& min, T const& max)
{
    return ::max(min, ::min(value, max));
}

// FIXME: This is a hack helper, remove it when no longer needed.
static CSS::Size to_css_size(CSS::LengthPercentage const& length_percentage)
{
    if (length_percentage.is_auto())
        return CSS::Size::make_auto();
    if (length_percentage.is_length())
        return CSS::Size::make_length(length_percentage.length());
    return CSS::Size::make_percentage(length_percentage.percentage());
}

float FlexFormattingContext::get_pixel_width(Box const& box, Optional<CSS::Size> const& size) const
{
    if (!size.has_value())
        return 0;
    auto inner_width = CSS::Length::make_px(containing_block_width_for(box));
    float border_left = box.computed_values().border_left().width;
    float border_right = box.computed_values().border_right().width;
    float padding_left = box.computed_values().padding().left().resolved(box, inner_width).to_px(box);
    float padding_right = box.computed_values().padding().right().resolved(box, inner_width).to_px(box);
    if (box.computed_values().box_sizing() == CSS::BoxSizing::BorderBox) {
        return size->resolved(box, inner_width).to_px(box) - border_left - border_right - padding_left - padding_right;
    }

    return size->resolved(box, inner_width).to_px(box);
}

float FlexFormattingContext::get_pixel_height(Box const& box, Optional<CSS::Size> const& length_percentage) const
{
    if (!length_percentage.has_value())
        return 0;
    auto inner_height = CSS::Length::make_px(containing_block_height_for(box));
    float border_top = box.computed_values().border_top().width;
    float border_bottom = box.computed_values().border_bottom().width;
    float padding_top = box.computed_values().padding().top().resolved(box, inner_height).to_px(box);
    float padding_bottom = box.computed_values().padding().bottom().resolved(box, inner_height).to_px(box);
    if (box.computed_values().box_sizing() == CSS::BoxSizing::BorderBox) {
        return length_percentage->resolved(box, inner_height).to_px(box) - border_top - border_bottom - padding_top - padding_bottom;
    }

    return length_percentage->resolved(box, inner_height).to_px(box);
}

FlexFormattingContext::FlexFormattingContext(LayoutState& state, Box const& flex_container, FormattingContext* parent)
    : FormattingContext(Type::Flex, state, flex_container, parent)
    , m_flex_container_state(m_state.get_mutable(flex_container))
    , m_flex_direction(flex_container.computed_values().flex_direction())
{
}

FlexFormattingContext::~FlexFormattingContext() = default;

float FlexFormattingContext::automatic_content_height() const
{
    return m_state.get(flex_container()).content_height();
}

void FlexFormattingContext::run(Box const& run_box, LayoutMode, AvailableSpace const& available_content_space)
{
    VERIFY(&run_box == &flex_container());

    // NOTE: The available space provided by the parent context is basically our *content box*.
    //       FFC is currently written in a way that expects that to include padding and border as well,
    //       so we pad out the available space here to accommodate that.
    // FIXME: Refactor the necessary parts of FFC so we don't need this hack!

    auto available_width = available_content_space.width;
    if (available_width.is_definite())
        available_width = AvailableSize::make_definite(available_width.to_px() + m_flex_container_state.border_box_left() + m_flex_container_state.border_box_right());
    auto available_height = available_content_space.height;
    if (available_height.is_definite())
        available_height = AvailableSize::make_definite(available_height.to_px() + m_flex_container_state.border_box_top() + m_flex_container_state.border_box_bottom());

    m_available_space_for_flex_container = AxisAgnosticAvailableSpace {
        .main = is_row_layout() ? available_width : available_height,
        .cross = !is_row_layout() ? available_width : available_height,
        .space = { available_width, available_height },
    };

    // This implements https://www.w3.org/TR/css-flexbox-1/#layout-algorithm

    // 1. Generate anonymous flex items
    generate_anonymous_flex_items();

    // 2. Determine the available main and cross space for the flex items
    determine_available_space_for_items(AvailableSpace(available_width, available_height));

    {
        // https://drafts.csswg.org/css-flexbox-1/#definite-sizes
        // 3. If a single-line flex container has a definite cross size,
        //    the automatic preferred outer cross size of any stretched flex items is the flex container’s inner cross size
        //    (clamped to the flex item’s min and max cross size) and is considered definite.
        if (is_single_line() && has_definite_cross_size(flex_container())) {
            auto flex_container_inner_cross_size = specified_cross_size(flex_container());
            for (auto& item : m_flex_items) {
                if (!flex_item_is_stretched(item))
                    continue;
                auto item_min_cross_size = has_cross_min_size(item.box) ? specified_cross_min_size(item.box) : automatic_minimum_size(item);
                auto item_max_cross_size = has_cross_max_size(item.box) ? specified_cross_max_size(item.box) : INFINITY;
                auto item_preferred_outer_cross_size = css_clamp(flex_container_inner_cross_size, item_min_cross_size, item_max_cross_size);
                auto item_inner_cross_size = item_preferred_outer_cross_size - item.margins.cross_before - item.margins.cross_after - item.padding.cross_before - item.padding.cross_after - item.borders.cross_before - item.borders.cross_after;
                set_cross_size(item.box, item_inner_cross_size);
            }
        }
    }

    // 3. Determine the flex base size and hypothetical main size of each item
    for (auto& flex_item : m_flex_items) {
        if (flex_item.box.is_replaced_box()) {
            // FIXME: Get rid of prepare_for_replaced_layout() and make replaced elements figure out their intrinsic size lazily.
            static_cast<ReplacedBox&>(flex_item.box).prepare_for_replaced_layout();
        }
        determine_flex_base_size_and_hypothetical_main_size(flex_item);
    }

    if (available_width.is_intrinsic_sizing_constraint() || available_height.is_intrinsic_sizing_constraint()) {
        // We're computing intrinsic size for the flex container.
        determine_intrinsic_size_of_flex_container();

        // Our caller is only interested in the content-width and content-height results,
        // which have now been set on m_flex_container_state, so there's no need to continue
        // the main layout algorithm after this point.
        return;
    }

    // 4. Determine the main size of the flex container
    determine_main_size_of_flex_container();

    // 5. Collect flex items into flex lines:
    // After this step no additional items are to be added to flex_lines or any of its items!
    collect_flex_items_into_flex_lines();

    // 6. Resolve the flexible lengths
    resolve_flexible_lengths();

    // Cross Size Determination
    // 7. Determine the hypothetical cross size of each item
    for (auto& flex_item : m_flex_items) {
        determine_hypothetical_cross_size_of_item(flex_item, false);
    }

    // 8. Calculate the cross size of each flex line.
    calculate_cross_size_of_each_flex_line();

    // 9. Handle 'align-content: stretch'.
    handle_align_content_stretch();

    // 10. Collapse visibility:collapse items.
    // FIXME: This

    // 11. Determine the used cross size of each flex item.
    determine_used_cross_size_of_each_flex_item();

    // 12. Distribute any remaining free space.
    distribute_any_remaining_free_space();

    // 13. Resolve cross-axis auto margins.
    resolve_cross_axis_auto_margins();

    // 14. Align all flex items along the cross-axis
    align_all_flex_items_along_the_cross_axis();

    // 15. Determine the flex container’s used cross size:
    determine_flex_container_used_cross_size();

    {
        // https://drafts.csswg.org/css-flexbox-1/#definite-sizes
        // 4. Once the cross size of a flex line has been determined,
        //    the cross sizes of items in auto-sized flex containers are also considered definite for the purpose of layout.
        auto const& flex_container_computed_cross_size = is_row_layout() ? flex_container().computed_values().height() : flex_container().computed_values().width();
        if (flex_container_computed_cross_size.is_auto()) {
            for (auto& item : m_flex_items) {
                set_cross_size(item.box, item.cross_size);
            }
        }
    }

    {
        // NOTE: We re-resolve cross sizes here, now that we can resolve percentages.

        // 7. Determine the hypothetical cross size of each item
        for (auto& flex_item : m_flex_items) {
            determine_hypothetical_cross_size_of_item(flex_item, true);
        }

        // 11. Determine the used cross size of each flex item.
        determine_used_cross_size_of_each_flex_item();
    }

    // 16. Align all flex lines (per align-content)
    align_all_flex_lines();

    // AD-HOC: Layout the inside of all flex items.
    copy_dimensions_from_flex_items_to_boxes();
    for (auto& flex_item : m_flex_items) {
        auto& box_state = m_state.get(flex_item.box);
        if (auto independent_formatting_context = layout_inside(flex_item.box, LayoutMode::Normal, box_state.available_inner_space_or_constraints_from(m_available_space_for_flex_container->space)))
            independent_formatting_context->parent_context_did_dimension_child_root_box();
    }

    // FIXME: We run the "copy dimensions" step *again* here, in order to override any sizes
    //        assigned to the flex item by the "layout inside" step above. This is definitely not
    //        part of the spec, and simply covering up the fact that our inside layout currently
    //        mutates the height of BFC roots.
    copy_dimensions_from_flex_items_to_boxes();
}

void FlexFormattingContext::parent_context_did_dimension_child_root_box()
{
    flex_container().for_each_child_of_type<Box>([&](Layout::Box& box) {
        if (box.is_absolutely_positioned()) {
            auto& cb_state = m_state.get(*box.containing_block());
            auto available_width = AvailableSize::make_definite(cb_state.content_width() + cb_state.padding_left + cb_state.padding_right);
            auto available_height = AvailableSize::make_definite(cb_state.content_height() + cb_state.padding_top + cb_state.padding_bottom);
            layout_absolutely_positioned_element(box, AvailableSpace(available_width, available_height));
        }
    });
}

void FlexFormattingContext::populate_specified_margins(FlexItem& item, CSS::FlexDirection flex_direction) const
{
    auto width_of_containing_block = m_state.get(*item.box.containing_block()).content_width();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
    // FIXME: This should also take reverse-ness into account
    if (flex_direction == CSS::FlexDirection::Row || flex_direction == CSS::FlexDirection::RowReverse) {
        item.borders.main_before = item.box.computed_values().border_left().width;
        item.borders.main_after = item.box.computed_values().border_right().width;
        item.borders.cross_before = item.box.computed_values().border_top().width;
        item.borders.cross_after = item.box.computed_values().border_bottom().width;

        item.padding.main_before = item.box.computed_values().padding().left().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.padding.main_after = item.box.computed_values().padding().right().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.padding.cross_before = item.box.computed_values().padding().top().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.padding.cross_after = item.box.computed_values().padding().bottom().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);

        item.margins.main_before = item.box.computed_values().margin().left().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.margins.main_after = item.box.computed_values().margin().right().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.margins.cross_before = item.box.computed_values().margin().top().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.margins.cross_after = item.box.computed_values().margin().bottom().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);

        item.margins.main_before_is_auto = item.box.computed_values().margin().left().is_auto();
        item.margins.main_after_is_auto = item.box.computed_values().margin().right().is_auto();
        item.margins.cross_before_is_auto = item.box.computed_values().margin().top().is_auto();
        item.margins.cross_after_is_auto = item.box.computed_values().margin().bottom().is_auto();
    } else {
        item.borders.main_before = item.box.computed_values().border_top().width;
        item.borders.main_after = item.box.computed_values().border_bottom().width;
        item.borders.cross_before = item.box.computed_values().border_left().width;
        item.borders.cross_after = item.box.computed_values().border_right().width;

        item.padding.main_before = item.box.computed_values().padding().top().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.padding.main_after = item.box.computed_values().padding().bottom().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.padding.cross_before = item.box.computed_values().padding().left().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.padding.cross_after = item.box.computed_values().padding().right().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);

        item.margins.main_before = item.box.computed_values().margin().top().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.margins.main_after = item.box.computed_values().margin().bottom().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.margins.cross_before = item.box.computed_values().margin().left().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);
        item.margins.cross_after = item.box.computed_values().margin().right().resolved(item.box, width_of_containing_block_as_length).to_px(item.box);

        item.margins.main_before_is_auto = item.box.computed_values().margin().top().is_auto();
        item.margins.main_after_is_auto = item.box.computed_values().margin().bottom().is_auto();
        item.margins.cross_before_is_auto = item.box.computed_values().margin().left().is_auto();
        item.margins.cross_after_is_auto = item.box.computed_values().margin().right().is_auto();
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
    HashMap<int, Vector<FlexItem>> order_item_bucket;

    flex_container().for_each_child_of_type<Box>([&](Box& child_box) {
        // Skip anonymous text runs that are only whitespace.
        if (child_box.is_anonymous() && !child_box.is_generated() && !child_box.first_child_of_type<BlockContainer>()) {
            bool contains_only_white_space = true;
            child_box.for_each_in_subtree([&](auto const& node) {
                if (!is<TextNode>(node) || !static_cast<TextNode const&>(node).dom_node().data().is_whitespace()) {
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

        auto& order_bucket = order_item_bucket.ensure(child_box.computed_values().order());
        order_bucket.append(move(flex_item));

        return IterationDecision::Continue;
    });

    auto keys = order_item_bucket.keys();

    if (is_direction_reverse()) {
        quick_sort(keys, [](auto& a, auto& b) { return a > b; });
    } else {
        quick_sort(keys, [](auto& a, auto& b) { return a < b; });
    }

    for (auto key : keys) {
        auto order_bucket = order_item_bucket.get(key);
        if (order_bucket.has_value()) {
            auto items = order_bucket.value();
            if (is_direction_reverse()) {
                for (auto flex_item : items.in_reverse()) {
                    m_flex_items.append(move(flex_item));
                }
            } else {
                for (auto flex_item : items) {
                    m_flex_items.append(move(flex_item));
                }
            }
        }
    }
}

bool FlexFormattingContext::has_definite_main_size(Box const& box) const
{
    auto const& used_values = m_state.get(box);
    return is_row_layout() ? used_values.has_definite_width() : used_values.has_definite_height();
}

float FlexFormattingContext::specified_main_size(Box const& box) const
{
    auto const& box_state = m_state.get(box);
    return is_row_layout() ? box_state.content_width() : box_state.content_height();
}

float FlexFormattingContext::specified_cross_size(Box const& box) const
{
    auto const& box_state = m_state.get(box);
    return is_row_layout() ? box_state.content_height() : box_state.content_width();
}

float FlexFormattingContext::resolved_definite_cross_size(FlexItem const& item) const
{
    return !is_row_layout() ? m_state.resolved_definite_width(item.box) : m_state.resolved_definite_height(item.box);
}

float FlexFormattingContext::resolved_definite_main_size(FlexItem const& item) const
{
    return is_row_layout() ? m_state.resolved_definite_width(item.box) : m_state.resolved_definite_height(item.box);
}

bool FlexFormattingContext::has_main_min_size(Box const& box) const
{
    auto const& value = is_row_layout() ? box.computed_values().min_width() : box.computed_values().min_height();
    return !value.is_auto();
}

bool FlexFormattingContext::has_cross_min_size(Box const& box) const
{
    auto const& value = is_row_layout() ? box.computed_values().min_height() : box.computed_values().min_width();
    return !value.is_auto();
}

bool FlexFormattingContext::has_definite_cross_size(Box const& box) const
{
    auto const& used_values = m_state.get(box);
    return is_row_layout() ? used_values.has_definite_height() : used_values.has_definite_width();
}

float FlexFormattingContext::specified_main_min_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_width(box, box.computed_values().min_width())
        : get_pixel_height(box, box.computed_values().min_height());
}

float FlexFormattingContext::specified_cross_min_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_height(box, box.computed_values().min_height())
        : get_pixel_width(box, box.computed_values().min_width());
}

bool FlexFormattingContext::has_main_max_size(Box const& box) const
{
    auto const& value = is_row_layout() ? box.computed_values().max_width() : box.computed_values().max_height();
    return !value.is_none();
}

bool FlexFormattingContext::has_cross_max_size(Box const& box) const
{
    auto const& value = !is_row_layout() ? box.computed_values().max_width() : box.computed_values().max_height();
    return !value.is_none();
}

float FlexFormattingContext::specified_main_max_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_width(box, box.computed_values().max_width())
        : get_pixel_height(box, box.computed_values().max_height());
}

float FlexFormattingContext::specified_cross_max_size(Box const& box) const
{
    return is_row_layout()
        ? get_pixel_height(box, box.computed_values().max_height())
        : get_pixel_width(box, box.computed_values().max_width());
}

bool FlexFormattingContext::is_cross_auto(Box const& box) const
{
    auto& cross_length = is_row_layout() ? box.computed_values().height() : box.computed_values().width();
    return cross_length.is_auto();
}

void FlexFormattingContext::set_main_size(Box const& box, float size)
{
    if (is_row_layout())
        m_state.get_mutable(box).set_content_width(size);
    else
        m_state.get_mutable(box).set_content_height(size);
}

void FlexFormattingContext::set_cross_size(Box const& box, float size)
{
    if (is_row_layout())
        m_state.get_mutable(box).set_content_height(size);
    else
        m_state.get_mutable(box).set_content_width(size);
}

void FlexFormattingContext::set_offset(Box const& box, float main_offset, float cross_offset)
{
    if (is_row_layout())
        m_state.get_mutable(box).offset = Gfx::FloatPoint { main_offset, cross_offset };
    else
        m_state.get_mutable(box).offset = Gfx::FloatPoint { cross_offset, main_offset };
}

void FlexFormattingContext::set_main_axis_first_margin(FlexItem& item, float margin)
{
    item.margins.main_before = margin;
    if (is_row_layout())
        m_state.get_mutable(item.box).margin_left = margin;
    else
        m_state.get_mutable(item.box).margin_top = margin;
}

void FlexFormattingContext::set_main_axis_second_margin(FlexItem& item, float margin)
{
    item.margins.main_after = margin;
    if (is_row_layout())
        m_state.get_mutable(item.box).margin_right = margin;
    else
        m_state.get_mutable(item.box).margin_bottom = margin;
}

// https://drafts.csswg.org/css-flexbox-1/#algo-available
void FlexFormattingContext::determine_available_space_for_items(AvailableSpace const& available_space)
{
    // For each dimension, if that dimension of the flex container’s content box is a definite size, use that;
    // if that dimension of the flex container is being sized under a min or max-content constraint, the available space in that dimension is that constraint;
    // otherwise, subtract the flex container’s margin, border, and padding from the space available to the flex container in that dimension and use that value.
    // This might result in an infinite value.

    Optional<AvailableSize> available_width_for_items;
    if (m_flex_container_state.has_definite_width()) {
        available_width_for_items = AvailableSize::make_definite(m_state.resolved_definite_width(flex_container()));
    } else {
        if (available_space.width.is_intrinsic_sizing_constraint()) {
            available_width_for_items = available_space.width;
        } else {
            if (available_space.width.is_definite()) {
                auto remaining = available_space.width.to_px()
                    - m_flex_container_state.margin_left
                    - m_flex_container_state.margin_right
                    - m_flex_container_state.border_left
                    - m_flex_container_state.padding_right
                    - m_flex_container_state.padding_left
                    - m_flex_container_state.padding_right;
                available_width_for_items = AvailableSize::make_definite(remaining);
            } else {
                available_width_for_items = AvailableSize::make_indefinite();
            }
        }
    }

    Optional<AvailableSize> available_height_for_items;
    if (m_flex_container_state.has_definite_height()) {
        available_height_for_items = AvailableSize::make_definite(m_state.resolved_definite_height(flex_container()));
    } else {
        if (available_space.height.is_intrinsic_sizing_constraint()) {
            available_height_for_items = available_space.height;
        } else {
            if (available_space.height.is_definite()) {
                auto remaining = available_space.height.to_px()
                    - m_flex_container_state.margin_top
                    - m_flex_container_state.margin_bottom
                    - m_flex_container_state.border_top
                    - m_flex_container_state.padding_bottom
                    - m_flex_container_state.padding_top
                    - m_flex_container_state.padding_bottom;
                available_height_for_items = AvailableSize::make_definite(remaining);
            } else {
                available_height_for_items = AvailableSize::make_indefinite();
            }
        }
    }

    if (is_row_layout()) {
        m_available_space_for_items = AxisAgnosticAvailableSpace {
            .main = *available_width_for_items,
            .cross = *available_height_for_items,
            .space = { *available_width_for_items, *available_height_for_items },
        };
    } else {
        m_available_space_for_items = AxisAgnosticAvailableSpace {
            .main = *available_height_for_items,
            .cross = *available_width_for_items,
            .space = { *available_width_for_items, *available_height_for_items },
        };
    }
}

float FlexFormattingContext::calculate_indefinite_main_size(FlexItem const& item)
{
    VERIFY(!has_definite_main_size(item.box));

    // Otherwise, size the item into the available space using its used flex basis in place of its main size,
    // treating a value of content as max-content.
    if (item.used_flex_basis.type == CSS::FlexBasis::Content)
        return calculate_max_content_main_size(item);

    // If a cross size is needed to determine the main size
    // (e.g. when the flex item’s main size is in its block axis, or when it has a preferred aspect ratio)
    // and the flex item’s cross size is auto and not definite,
    // in this calculation use fit-content as the flex item’s cross size.
    // The flex base size is the item’s resulting main size.

    bool main_size_is_in_block_axis = !is_row_layout();
    // FIXME: Figure out if we have a preferred aspect ratio.
    bool has_preferred_aspect_ratio = false;

    bool cross_size_needed_to_determine_main_size = main_size_is_in_block_axis || has_preferred_aspect_ratio;

    if (cross_size_needed_to_determine_main_size) {
        // Figure out the fit-content cross size, then layout with that and see what height comes out of it.
        float fit_content_cross_size = calculate_fit_content_cross_size(item);

        LayoutState throwaway_state(&m_state);
        auto& box_state = throwaway_state.get_mutable(item.box);

        // Item has definite cross size, layout with that as the used cross size.
        auto independent_formatting_context = create_independent_formatting_context_if_needed(throwaway_state, item.box);
        // NOTE: Flex items should always create an independent formatting context!
        VERIFY(independent_formatting_context);

        box_state.set_content_width(fit_content_cross_size);
        independent_formatting_context->run(item.box, LayoutMode::Normal, m_available_space_for_items->space);

        return independent_formatting_context->automatic_content_height();
    }

    return calculate_fit_content_main_size(item);
}

// https://drafts.csswg.org/css-flexbox-1/#propdef-flex-basis
CSS::FlexBasisData FlexFormattingContext::used_flex_basis_for_item(FlexItem const& item) const
{
    auto flex_basis = item.box.computed_values().flex_basis();

    if (flex_basis.type == CSS::FlexBasis::Auto) {
        // https://drafts.csswg.org/css-flexbox-1/#valdef-flex-basis-auto
        // When specified on a flex item, the auto keyword retrieves the value of the main size property as the used flex-basis.
        // If that value is itself auto, then the used value is content.
        auto const& main_size = is_row_layout() ? item.box.computed_values().width() : item.box.computed_values().height();

        if (main_size.is_auto()) {
            flex_basis.type = CSS::FlexBasis::Content;
        } else {
            flex_basis.type = CSS::FlexBasis::LengthPercentage;
            if (main_size.is_length()) {
                flex_basis.length_percentage = main_size.length();
            } else if (main_size.is_percentage()) {
                flex_basis.length_percentage = main_size.percentage();
            } else {
                // FIXME: Support other size values!
                dbgln("FIXME: Unsupported main size for flex-basis!");
                flex_basis.type = CSS::FlexBasis::Content;
            }
        }
    }

    return flex_basis;
}

// https://www.w3.org/TR/css-flexbox-1/#algo-main-item
void FlexFormattingContext::determine_flex_base_size_and_hypothetical_main_size(FlexItem& flex_item)
{
    auto& child_box = flex_item.box;

    flex_item.flex_base_size = [&] {
        flex_item.used_flex_basis = used_flex_basis_for_item(flex_item);

        flex_item.used_flex_basis_is_definite = [&](CSS::FlexBasisData const& flex_basis) -> bool {
            if (flex_basis.type != CSS::FlexBasis::LengthPercentage)
                return false;
            if (flex_basis.length_percentage->is_auto())
                return false;
            if (flex_basis.length_percentage->is_length())
                return true;
            if (flex_basis.length_percentage->is_calculated()) {
                // FIXME: Handle calc() in used flex basis.
                return false;
            }
            if (is_row_layout())
                return m_flex_container_state.has_definite_width();
            return m_flex_container_state.has_definite_height();
        }(flex_item.used_flex_basis);

        // A. If the item has a definite used flex basis, that’s the flex base size.
        if (flex_item.used_flex_basis_is_definite) {
            if (is_row_layout())
                return get_pixel_width(child_box, to_css_size(flex_item.used_flex_basis.length_percentage.value()));
            return get_pixel_height(child_box, to_css_size(flex_item.used_flex_basis.length_percentage.value()));
        }

        // B. If the flex item has ...
        //    - an intrinsic aspect ratio,
        //    - a used flex basis of content, and
        //    - a definite cross size,
        if (flex_item.box.has_intrinsic_aspect_ratio()
            && flex_item.used_flex_basis.type == CSS::FlexBasis::Content
            && has_definite_cross_size(flex_item.box)) {
            // flex_base_size is calculated from definite cross size and intrinsic aspect ratio
            return resolved_definite_cross_size(flex_item) * flex_item.box.intrinsic_aspect_ratio().value();
        }

        // C. If the used flex basis is content or depends on its available space,
        //    and the flex container is being sized under a min-content or max-content constraint
        //    (e.g. when performing automatic table layout [CSS21]), size the item under that constraint.
        //    The flex base size is the item’s resulting main size.
        if (flex_item.used_flex_basis.type == CSS::FlexBasis::Content && m_available_space_for_items->main.is_intrinsic_sizing_constraint()) {
            if (m_available_space_for_items->main.is_min_content())
                return calculate_min_content_main_size(flex_item);
            return calculate_max_content_main_size(flex_item);
        }

        // D. Otherwise, if the used flex basis is content or depends on its available space,
        //    the available main size is infinite, and the flex item’s inline axis is parallel to the main axis,
        //    lay the item out using the rules for a box in an orthogonal flow [CSS3-WRITING-MODES].
        //    The flex base size is the item’s max-content main size.
        if (flex_item.used_flex_basis.type == CSS::FlexBasis::Content
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
            return resolved_definite_main_size(flex_item);

        return calculate_indefinite_main_size(flex_item);
    }();

    // The hypothetical main size is the item’s flex base size clamped according to its used min and max main sizes (and flooring the content box size at zero).
    auto clamp_min = has_main_min_size(child_box) ? specified_main_min_size(child_box) : automatic_minimum_size(flex_item);
    auto clamp_max = has_main_max_size(child_box) ? specified_main_max_size(child_box) : NumericLimits<float>::max();
    flex_item.hypothetical_main_size = max(0.0f, css_clamp(flex_item.flex_base_size, clamp_min, clamp_max));

    // NOTE: At this point, we set the hypothetical main size as the flex item's *temporary* main size.
    //       The size may change again when we resolve flexible lengths, but this is necessary in order for
    //       descendants of this flex item to resolve percentage sizes against something.
    //
    //       The spec just barely hand-waves about this, but it seems to *roughly* match what other engines do.
    //       See "Note" section here: https://drafts.csswg.org/css-flexbox-1/#definite-sizes
    if (is_row_layout())
        m_state.get_mutable(flex_item.box).set_temporary_content_width(flex_item.hypothetical_main_size);
    else
        m_state.get_mutable(flex_item.box).set_temporary_content_height(flex_item.hypothetical_main_size);
}

// https://drafts.csswg.org/css-flexbox-1/#min-size-auto
float FlexFormattingContext::automatic_minimum_size(FlexItem const& item) const
{
    // FIXME: Deal with scroll containers.
    return content_based_minimum_size(item);
}

// https://drafts.csswg.org/css-flexbox-1/#specified-size-suggestion
Optional<float> FlexFormattingContext::specified_size_suggestion(FlexItem const& item) const
{
    // If the item’s preferred main size is definite and not automatic,
    // then the specified size suggestion is that size. It is otherwise undefined.
    if (has_definite_main_size(item.box))
        return specified_main_size(item.box);
    return {};
}

// https://drafts.csswg.org/css-flexbox-1/#content-size-suggestion
float FlexFormattingContext::content_size_suggestion(FlexItem const& item) const
{
    // FIXME: Apply clamps
    return calculate_min_content_main_size(item);
}

// https://drafts.csswg.org/css-flexbox-1/#transferred-size-suggestion
Optional<float> FlexFormattingContext::transferred_size_suggestion(FlexItem const& item) const
{
    // If the item has a preferred aspect ratio and its preferred cross size is definite,
    // then the transferred size suggestion is that size
    // (clamped by its minimum and maximum cross sizes if they are definite), converted through the aspect ratio.
    if (item.box.has_intrinsic_aspect_ratio() && has_definite_cross_size(item.box)) {
        auto aspect_ratio = item.box.intrinsic_aspect_ratio().value();
        // FIXME: Clamp cross size to min/max cross size before this conversion.
        return resolved_definite_cross_size(item) * aspect_ratio;
    }

    // It is otherwise undefined.
    return {};
}

// https://drafts.csswg.org/css-flexbox-1/#content-based-minimum-size
float FlexFormattingContext::content_based_minimum_size(FlexItem const& item) const
{
    auto unclamped_size = [&] {
        // The content-based minimum size of a flex item is the smaller of its specified size suggestion
        // and its content size suggestion if its specified size suggestion exists;
        if (auto specified_size_suggestion = this->specified_size_suggestion(item); specified_size_suggestion.has_value()) {
            return min(specified_size_suggestion.value(), content_size_suggestion(item));
        }

        // otherwise, the smaller of its transferred size suggestion and its content size suggestion
        // if the element is replaced and its transferred size suggestion exists;
        if (item.box.is_replaced_box()) {
            if (auto transferred_size_suggestion = this->transferred_size_suggestion(item); transferred_size_suggestion.has_value()) {
                return min(transferred_size_suggestion.value(), content_size_suggestion(item));
            }
        }

        // otherwise its content size suggestion.
        return content_size_suggestion(item);
    }();

    // In all cases, the size is clamped by the maximum main size if it’s definite.
    if (has_main_max_size(item.box)) {
        return min(unclamped_size, specified_main_max_size(item.box));
    }
    return unclamped_size;
}

bool FlexFormattingContext::can_determine_size_of_child() const
{
    return true;
}

void FlexFormattingContext::determine_width_of_child(Box const&, AvailableSpace const&)
{
    // NOTE: For now, we simply do nothing here. If a child context is calling up to us
    //       and asking us to determine its width, we've already done so as part of the
    //       flex layout algorithm.
}

void FlexFormattingContext::determine_height_of_child(Box const&, AvailableSpace const&)
{
    // NOTE: For now, we simply do nothing here. If a child context is calling up to us
    //       and asking us to determine its height, we've already done so as part of the
    //       flex layout algorithm.
}

// https://drafts.csswg.org/css-flexbox-1/#algo-main-container
void FlexFormattingContext::determine_main_size_of_flex_container()
{
    // Determine the main size of the flex container using the rules of the formatting context in which it participates.
    // NOTE: The automatic block size of a block-level flex container is its max-content size.

    // FIXME: The code below doesn't know how to size absolutely positioned flex containers at all.
    //        We just leave it alone for now and let the parent context deal with it.
    if (flex_container().is_absolutely_positioned())
        return;

    // FIXME: Once all parent contexts now how to size a given child, we can remove
    //        `can_determine_size_of_child()`.
    if (parent()->can_determine_size_of_child()) {
        if (is_row_layout()) {
            parent()->determine_width_of_child(flex_container(), m_available_space_for_flex_container->space);
        } else {
            parent()->determine_height_of_child(flex_container(), m_available_space_for_flex_container->space);
        }
        return;
    }

    if (is_row_layout()) {
        if (!flex_container().is_out_of_flow(*parent()) && m_state.get(*flex_container().containing_block()).has_definite_width()) {
            set_main_size(flex_container(), calculate_stretch_fit_width(flex_container(), m_available_space_for_flex_container->space.width));
        } else {
            set_main_size(flex_container(), calculate_max_content_width(flex_container()));
        }
    } else {
        if (!has_definite_main_size(flex_container()))
            set_main_size(flex_container(), calculate_max_content_height(flex_container(), m_available_space_for_flex_container->space.width));
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-line-break
void FlexFormattingContext::collect_flex_items_into_flex_lines()
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
        auto outer_hypothetical_main_size = flex_item.hypothetical_main_size + flex_item.margins.main_before + flex_item.margins.main_after + flex_item.borders.main_before + flex_item.borders.main_after + flex_item.padding.main_before + flex_item.padding.main_after;
        if (!line.items.is_empty() && (line_main_size + outer_hypothetical_main_size) > specified_main_size(flex_container())) {
            m_flex_lines.append(move(line));
            line = {};
            line_main_size = 0;
        }
        line.items.append(&flex_item);
        line_main_size += outer_hypothetical_main_size;
    }
    m_flex_lines.append(move(line));
}

// https://www.w3.org/TR/css-flexbox-1/#resolve-flexible-lengths
void FlexFormattingContext::resolve_flexible_lengths()
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
            sum_of_hypothetical_main_sizes += (flex_item->hypothetical_main_size + flex_item->margins.main_before + flex_item->margins.main_after + flex_item->borders.main_before + flex_item->borders.main_after + flex_item->padding.main_before + flex_item->padding.main_after);
        }
        if (sum_of_hypothetical_main_sizes < specified_main_size(flex_container()))
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
                    sum_of_items_on_line += flex_item->target_main_size + flex_item->margins.main_before + flex_item->margins.main_after + flex_item->borders.main_before + flex_item->borders.main_after + flex_item->padding.main_before + flex_item->padding.main_after;
                else
                    sum_of_items_on_line += flex_item->flex_base_size + flex_item->margins.main_before + flex_item->margins.main_after + flex_item->borders.main_before + flex_item->borders.main_after + flex_item->padding.main_before + flex_item->padding.main_after;
            }
            return specified_main_size(flex_container()) - sum_of_items_on_line;
        };

        float initial_free_space = calculate_free_space();
        flex_line.remaining_free_space = initial_free_space;

        // 6.4 Loop
        auto for_each_unfrozen_item = [&flex_line](auto callback) {
            for (auto& flex_item : flex_line.items) {
                if (!flex_item->frozen)
                    callback(flex_item);
            }
        };

        while (number_of_unfrozen_items_on_line > 0) {
            // b Calculate the remaining free space
            flex_line.remaining_free_space = calculate_free_space();
            float sum_of_unfrozen_flex_items_flex_factors = 0;
            for_each_unfrozen_item([&](FlexItem* item) {
                sum_of_unfrozen_flex_items_flex_factors += item->flex_factor.value_or(1);
            });

            if (sum_of_unfrozen_flex_items_flex_factors < 1) {
                auto intermediate_free_space = initial_free_space * sum_of_unfrozen_flex_items_flex_factors;
                if (AK::abs(intermediate_free_space) < AK::abs(flex_line.remaining_free_space))
                    flex_line.remaining_free_space = intermediate_free_space;
            }

            // c Distribute free space proportional to the flex factors
            if (flex_line.remaining_free_space != 0) {
                if (used_flex_factor == FlexFactor::FlexGrowFactor) {
                    float sum_of_flex_grow_factor_of_unfrozen_items = sum_of_unfrozen_flex_items_flex_factors;
                    for_each_unfrozen_item([&](FlexItem* flex_item) {
                        float ratio = flex_item->flex_factor.value_or(1) / sum_of_flex_grow_factor_of_unfrozen_items;
                        flex_item->target_main_size = flex_item->flex_base_size + (flex_line.remaining_free_space * ratio);
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
                        flex_item->target_main_size = flex_item->flex_base_size - (AK::abs(flex_line.remaining_free_space) * ratio);
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
                    : automatic_minimum_size(*item);
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
            set_main_size(flex_item->box, flex_item->main_size);
        }

        flex_line.remaining_free_space = calculate_free_space();
    }
}

// https://drafts.csswg.org/css-flexbox-1/#algo-cross-item
void FlexFormattingContext::determine_hypothetical_cross_size_of_item(FlexItem& item, bool resolve_percentage_min_max_sizes)
{
    // Determine the hypothetical cross size of each item by performing layout
    // as if it were an in-flow block-level box with the used main size
    // and the given available space, treating auto as fit-content.

    auto const& computed_min_size = this->computed_cross_min_size(item.box);
    auto const& computed_max_size = this->computed_cross_max_size(item.box);

    auto clamp_min = (!computed_min_size.is_auto() && (resolve_percentage_min_max_sizes || !computed_min_size.contains_percentage())) ? specified_cross_min_size(item.box) : 0;
    auto clamp_max = (!computed_max_size.is_none() && (resolve_percentage_min_max_sizes || !computed_max_size.contains_percentage())) ? specified_cross_max_size(item.box) : NumericLimits<float>::max();

    // If we have a definite cross size, this is easy! No need to perform layout, we can just use it as-is.
    if (has_definite_cross_size(item.box)) {
        // To avoid subtracting padding and border twice for `box-sizing: border-box` only min and max clamp should happen on a second pass
        if (resolve_percentage_min_max_sizes) {
            item.hypothetical_cross_size = css_clamp(item.hypothetical_cross_size, clamp_min, clamp_max);
            return;
        }

        float cross_size = [&]() {
            if (item.box.computed_values().box_sizing() == CSS::BoxSizing::BorderBox) {
                return max(0.0f, resolved_definite_cross_size(item) - item.padding.cross_before - item.padding.cross_after - item.borders.cross_before - item.borders.cross_after);
            }

            return resolved_definite_cross_size(item);
        }();

        item.hypothetical_cross_size = css_clamp(cross_size, clamp_min, clamp_max);
        return;
    }

    if (computed_cross_size(item.box).is_auto()) {
        // Item has automatic cross size, layout with "fit-content"
        item.hypothetical_cross_size = css_clamp(calculate_fit_content_cross_size(item), clamp_min, clamp_max);
        return;
    }

    // For indefinite cross sizes, we perform a throwaway layout and then measure it.
    LayoutState throwaway_state(&m_state);

    auto& containing_block_state = throwaway_state.get_mutable(flex_container());
    if (is_row_layout()) {
        containing_block_state.set_content_width(item.main_size);
    } else {
        containing_block_state.set_content_height(item.main_size);
    }

    auto& box_state = throwaway_state.get_mutable(item.box);

    // Item has definite main size, layout with that as the used main size.
    auto independent_formatting_context = create_independent_formatting_context_if_needed(throwaway_state, item.box);
    // NOTE: Flex items should always create an independent formatting context!
    VERIFY(independent_formatting_context);

    independent_formatting_context->run(item.box, LayoutMode::Normal, m_available_space_for_items->space);

    auto automatic_cross_size = is_row_layout() ? independent_formatting_context->automatic_content_height()
                                                : box_state.content_width();

    item.hypothetical_cross_size = css_clamp(automatic_cross_size, clamp_min, clamp_max);
}

// https://www.w3.org/TR/css-flexbox-1/#algo-cross-line
void FlexFormattingContext::calculate_cross_size_of_each_flex_line()
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
    if (is_single_line()) {
        auto const& computed_min_size = this->computed_cross_min_size(flex_container());
        auto const& computed_max_size = this->computed_cross_max_size(flex_container());
        auto cross_min_size = (!computed_min_size.is_auto() && !computed_min_size.contains_percentage()) ? specified_cross_min_size(flex_container()) : 0;
        auto cross_max_size = (!computed_max_size.is_none() && !computed_max_size.contains_percentage()) ? specified_cross_max_size(flex_container()) : INFINITY;
        m_flex_lines[0].cross_size = css_clamp(m_flex_lines[0].cross_size, cross_min_size, cross_max_size);
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-stretch
void FlexFormattingContext::determine_used_cross_size_of_each_flex_item()
{
    for (auto& flex_line : m_flex_lines) {
        for (auto& flex_item : flex_line.items) {
            //  If a flex item has align-self: stretch, its computed cross size property is auto,
            //  and neither of its cross-axis margins are auto, the used outer cross size is the used cross size of its flex line,
            //  clamped according to the item’s used min and max cross sizes.
            if (alignment_for_item(flex_item->box) == CSS::AlignItems::Stretch
                && is_cross_auto(flex_item->box)
                && !flex_item->margins.cross_before_is_auto
                && !flex_item->margins.cross_after_is_auto) {
                auto unclamped_cross_size = flex_line.cross_size
                    - flex_item->margins.cross_before - flex_item->margins.cross_after
                    - flex_item->padding.cross_before - flex_item->padding.cross_after
                    - flex_item->borders.cross_before - flex_item->borders.cross_after;

                auto const& computed_min_size = computed_cross_min_size(flex_item->box);
                auto const& computed_max_size = computed_cross_max_size(flex_item->box);
                auto cross_min_size = (!computed_min_size.is_auto() && !computed_min_size.contains_percentage()) ? specified_cross_min_size(flex_item->box) : 0;
                auto cross_max_size = (!computed_max_size.is_none() && !computed_max_size.contains_percentage()) ? specified_cross_max_size(flex_item->box) : INFINITY;

                flex_item->cross_size = css_clamp(unclamped_cross_size, cross_min_size, cross_max_size);
            } else {
                // Otherwise, the used cross size is the item’s hypothetical cross size.
                flex_item->cross_size = flex_item->hypothetical_cross_size;
            }
        }
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-main-align
void FlexFormattingContext::distribute_any_remaining_free_space()
{
    for (auto& flex_line : m_flex_lines) {
        // 12.1.
        float used_main_space = 0;
        size_t auto_margins = 0;
        for (auto& flex_item : flex_line.items) {
            used_main_space += flex_item->main_size;
            if (flex_item->margins.main_before_is_auto)
                ++auto_margins;

            if (flex_item->margins.main_after_is_auto)
                ++auto_margins;

            used_main_space += flex_item->margins.main_before + flex_item->margins.main_after
                + flex_item->borders.main_before + flex_item->borders.main_after
                + flex_item->padding.main_before + flex_item->padding.main_after;
        }

        if (flex_line.remaining_free_space > 0) {
            float size_per_auto_margin = flex_line.remaining_free_space / (float)auto_margins;
            for (auto& flex_item : flex_line.items) {
                if (flex_item->margins.main_before_is_auto)
                    set_main_axis_first_margin(*flex_item, size_per_auto_margin);
                if (flex_item->margins.main_after_is_auto)
                    set_main_axis_second_margin(*flex_item, size_per_auto_margin);
            }
        } else {
            for (auto& flex_item : flex_line.items) {
                if (flex_item->margins.main_before_is_auto)
                    set_main_axis_first_margin(*flex_item, 0);
                if (flex_item->margins.main_after_is_auto)
                    set_main_axis_second_margin(*flex_item, 0);
            }
        }

        // 12.2.
        float space_between_items = 0;
        float initial_offset = 0;
        auto number_of_items = flex_line.items.size();

        enum class FlexRegionRenderCursor {
            Left,
            Right
        };

        auto flex_region_render_cursor = FlexRegionRenderCursor::Left;
        bool justification_is_centered = false;

        switch (flex_container().computed_values().justify_content()) {
        case CSS::JustifyContent::FlexStart:
            if (is_direction_reverse()) {
                flex_region_render_cursor = FlexRegionRenderCursor::Right;
                initial_offset = specified_main_size(flex_container());
            } else {
                initial_offset = 0;
            }
            break;
        case CSS::JustifyContent::FlexEnd:
            if (is_direction_reverse()) {
                initial_offset = 0;
            } else {
                flex_region_render_cursor = FlexRegionRenderCursor::Right;
                initial_offset = specified_main_size(flex_container());
            }
            break;
        case CSS::JustifyContent::Center:
            initial_offset = (specified_main_size(flex_container()) - used_main_space) / 2.0f;
            justification_is_centered = true;
            break;
        case CSS::JustifyContent::SpaceBetween:
            space_between_items = flex_line.remaining_free_space / (number_of_items - 1);
            break;
        case CSS::JustifyContent::SpaceAround:
            space_between_items = flex_line.remaining_free_space / number_of_items;
            initial_offset = space_between_items / 2.0f;
            justification_is_centered = true;
            break;
        }

        // For reverse, we use FlexRegionRenderCursor::Right
        // to indicate the cursor offset is the end and render backwards
        // Otherwise the cursor offset is the 'start' of the region or initial offset
        float cursor_offset = initial_offset;

        auto place_item = [&](FlexItem& item, bool is_first_item, bool is_last_item) {
            // NOTE: For centered justifications (`center` and `space-around`) we ignore any margin
            //       before the first item, and after the last item.

            float item_margin_before = item.margins.main_before;
            float item_margin_after = item.margins.main_after;
            if (justification_is_centered) {
                if (is_first_item)
                    item_margin_before = 0;
                if (is_last_item)
                    item_margin_after = 0;
            }

            auto amount_of_main_size_used = item.main_size
                + item_margin_before
                + item.borders.main_before
                + item.padding.main_before
                + item_margin_after
                + item.borders.main_after
                + item.padding.main_after
                + space_between_items;

            if (is_direction_reverse()) {
                item.main_offset = cursor_offset - item.main_size - item_margin_after - item.borders.main_after - item.padding.main_after;
                cursor_offset -= amount_of_main_size_used;
            } else if (flex_region_render_cursor == FlexRegionRenderCursor::Right) {
                cursor_offset -= amount_of_main_size_used;
                item.main_offset = cursor_offset + item_margin_before + item.borders.main_before + item.padding.main_before;
            } else {
                item.main_offset = cursor_offset + item_margin_before + item.borders.main_before + item.padding.main_before;
                cursor_offset += amount_of_main_size_used;
            }
        };

        if (is_direction_reverse() || flex_region_render_cursor == FlexRegionRenderCursor::Right) {
            for (ssize_t i = flex_line.items.size() - 1; i >= 0; --i) {
                auto& item = flex_line.items[i];
                place_item(*item, i == static_cast<ssize_t>(flex_line.items.size()) - 1, i == 0);
            }
        } else {
            for (size_t i = 0; i < flex_line.items.size(); ++i) {
                auto& item = flex_line.items[i];
                place_item(*item, i == 0, i == flex_line.items.size() - 1);
            }
        }
    }
}

void FlexFormattingContext::dump_items() const
{
    dbgln("\033[34;1mflex-container\033[0m {}, direction: {}, current-size: {}x{}", flex_container().debug_description(), is_row_layout() ? "row" : "column", m_flex_container_state.content_width(), m_flex_container_state.content_height());
    for (size_t i = 0; i < m_flex_lines.size(); ++i) {
        dbgln("{} flex-line #{}:", flex_container().debug_description(), i);
        for (size_t j = 0; j < m_flex_lines[i].items.size(); ++j) {
            auto& item = *m_flex_lines[i].items[j];
            dbgln("{}   flex-item #{}: {} (main:{}, cross:{})", flex_container().debug_description(), j, item.box.debug_description(), item.main_size, item.cross_size);
        }
    }
}

CSS::AlignItems FlexFormattingContext::alignment_for_item(Box const& box) const
{
    switch (box.computed_values().align_self()) {
    case CSS::AlignSelf::Auto:
        return flex_container().computed_values().align_items();
    case CSS::AlignSelf::Normal:
        return CSS::AlignItems::Normal;
    case CSS::AlignSelf::SelfStart:
        return CSS::AlignItems::SelfStart;
    case CSS::AlignSelf::SelfEnd:
        return CSS::AlignItems::SelfEnd;
    case CSS::AlignSelf::FlexStart:
        return CSS::AlignItems::FlexStart;
    case CSS::AlignSelf::FlexEnd:
        return CSS::AlignItems::FlexEnd;
    case CSS::AlignSelf::Center:
        return CSS::AlignItems::Center;
    case CSS::AlignSelf::Baseline:
        return CSS::AlignItems::Baseline;
    case CSS::AlignSelf::Stretch:
        return CSS::AlignItems::Stretch;
    case CSS::AlignSelf::Safe:
        return CSS::AlignItems::Safe;
    case CSS::AlignSelf::Unsafe:
        return CSS::AlignItems::Unsafe;
    default:
        VERIFY_NOT_REACHED();
    }
}

void FlexFormattingContext::align_all_flex_items_along_the_cross_axis()
{
    // FIXME: Take better care of margins
    for (auto& flex_line : m_flex_lines) {
        for (auto* flex_item : flex_line.items) {
            float half_line_size = flex_line.cross_size / 2.0f;
            switch (alignment_for_item(flex_item->box)) {
            case CSS::AlignItems::Baseline:
                // FIXME: Implement this
                //  Fallthrough
            case CSS::AlignItems::FlexStart:
            case CSS::AlignItems::Stretch:
                flex_item->cross_offset = 0 - half_line_size + flex_item->margins.cross_before + flex_item->borders.cross_before + flex_item->padding.cross_before;
                break;
            case CSS::AlignItems::FlexEnd:
                flex_item->cross_offset = half_line_size - flex_item->cross_size - flex_item->margins.cross_after - flex_item->borders.cross_after - flex_item->padding.cross_after;
                break;
            case CSS::AlignItems::Center:
                flex_item->cross_offset = 0 - (flex_item->cross_size / 2.0f);
                break;
            default:
                break;
            }
        }
    }
}

// https://www.w3.org/TR/css-flexbox-1/#algo-cross-container
void FlexFormattingContext::determine_flex_container_used_cross_size()
{
    float cross_size = 0;
    if (has_definite_cross_size(flex_container())) {
        // Flex container has definite cross size: easy-peasy.
        cross_size = specified_cross_size(flex_container());
    } else {
        // Flex container has indefinite cross size.
        auto cross_size_value = is_row_layout() ? flex_container().computed_values().height() : flex_container().computed_values().width();
        if (cross_size_value.is_auto() || cross_size_value.contains_percentage()) {
            // If a content-based cross size is needed, use the sum of the flex lines' cross sizes.
            float sum_of_flex_lines_cross_sizes = 0;
            for (auto& flex_line : m_flex_lines) {
                sum_of_flex_lines_cross_sizes += flex_line.cross_size;
            }
            cross_size = sum_of_flex_lines_cross_sizes;

            if (cross_size_value.contains_percentage()) {
                // FIXME: Handle percentage values here! Right now we're just treating them as "auto"
            }
        } else {
            // Otherwise, resolve the indefinite size at this point.
            cross_size = cross_size_value.resolved(flex_container(), CSS::Length::make_px(specified_cross_size(*flex_container().containing_block()))).to_px(flex_container());
        }
    }
    auto const& computed_min_size = this->computed_cross_min_size(flex_container());
    auto const& computed_max_size = this->computed_cross_max_size(flex_container());
    auto cross_min_size = (!computed_min_size.is_auto() && !computed_min_size.contains_percentage()) ? specified_cross_min_size(flex_container()) : 0;
    auto cross_max_size = (!computed_max_size.is_none() && !computed_max_size.contains_percentage()) ? specified_cross_max_size(flex_container()) : INFINITY;
    set_cross_size(flex_container(), css_clamp(cross_size, cross_min_size, cross_max_size));
}

// https://www.w3.org/TR/css-flexbox-1/#algo-line-align
void FlexFormattingContext::align_all_flex_lines()
{
    if (m_flex_lines.is_empty())
        return;

    // FIXME: Support reverse

    float cross_size_of_flex_container = specified_cross_size(flex_container());

    if (is_single_line()) {
        // For single-line flex containers, we only need to center the line along the cross axis.
        auto& flex_line = m_flex_lines[0];
        float center_of_line = cross_size_of_flex_container / 2.0f;
        for (auto* flex_item : flex_line.items) {
            flex_item->cross_offset += center_of_line;
        }
    } else {

        float sum_of_flex_line_cross_sizes = 0;
        for (auto& line : m_flex_lines)
            sum_of_flex_line_cross_sizes += line.cross_size;

        float start_of_current_line = 0;
        float gap_size = 0;
        switch (flex_container().computed_values().align_content()) {
        case CSS::AlignContent::FlexStart:
            start_of_current_line = 0;
            break;
        case CSS::AlignContent::FlexEnd:
            start_of_current_line = cross_size_of_flex_container - sum_of_flex_line_cross_sizes;
            break;
        case CSS::AlignContent::Center:
            start_of_current_line = (cross_size_of_flex_container / 2) - (sum_of_flex_line_cross_sizes / 2);
            break;
        case CSS::AlignContent::SpaceBetween: {
            start_of_current_line = 0;

            float leftover_free_space = cross_size_of_flex_container - sum_of_flex_line_cross_sizes;
            if (leftover_free_space >= 0) {
                int gap_count = m_flex_lines.size() - 1;
                gap_size = leftover_free_space / gap_count;
            }
            break;
        }
        case CSS::AlignContent::SpaceAround: {
            float leftover_free_space = cross_size_of_flex_container - sum_of_flex_line_cross_sizes;
            if (leftover_free_space < 0) {
                // If the leftover free-space is negative this value is identical to center.
                start_of_current_line = (cross_size_of_flex_container / 2) - (sum_of_flex_line_cross_sizes / 2);
                break;
            }

            gap_size = leftover_free_space / m_flex_lines.size();

            // The spacing between the first/last lines and the flex container edges is half the size of the spacing between flex lines.
            start_of_current_line = gap_size / 2;
            break;
        }
        case CSS::AlignContent::Stretch:
            start_of_current_line = 0;
            break;
        }

        for (auto& flex_line : m_flex_lines) {
            float center_of_current_line = start_of_current_line + (flex_line.cross_size / 2);
            for (auto* flex_item : flex_line.items) {
                flex_item->cross_offset += center_of_current_line;
            }
            start_of_current_line += flex_line.cross_size + gap_size;
        }
    }
}

void FlexFormattingContext::copy_dimensions_from_flex_items_to_boxes()
{
    for (auto& flex_item : m_flex_items) {
        auto const& box = flex_item.box;
        auto& box_state = m_state.get_mutable(box);

        box_state.padding_left = box.computed_values().padding().left().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);
        box_state.padding_right = box.computed_values().padding().right().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);
        box_state.padding_top = box.computed_values().padding().top().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);
        box_state.padding_bottom = box.computed_values().padding().bottom().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);

        box_state.margin_left = box.computed_values().margin().left().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);
        box_state.margin_right = box.computed_values().margin().right().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);
        box_state.margin_top = box.computed_values().margin().top().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);
        box_state.margin_bottom = box.computed_values().margin().bottom().resolved(box, CSS::Length::make_px(m_flex_container_state.content_width())).to_px(box);

        box_state.border_left = box.computed_values().border_left().width;
        box_state.border_right = box.computed_values().border_right().width;
        box_state.border_top = box.computed_values().border_top().width;
        box_state.border_bottom = box.computed_values().border_bottom().width;

        set_main_size(box, flex_item.main_size);
        set_cross_size(box, flex_item.cross_size);
        set_offset(box, flex_item.main_offset, flex_item.cross_offset);
    }
}

// https://drafts.csswg.org/css-flexbox-1/#intrinsic-sizes
void FlexFormattingContext::determine_intrinsic_size_of_flex_container()
{
    if (m_available_space_for_flex_container->main.is_intrinsic_sizing_constraint()) {
        float main_size = calculate_intrinsic_main_size_of_flex_container();
        set_main_size(flex_container(), main_size);
    }
    if (m_available_space_for_items->cross.is_intrinsic_sizing_constraint()) {
        float cross_size = calculate_intrinsic_cross_size_of_flex_container();
        set_cross_size(flex_container(), cross_size);
    }
}

// https://drafts.csswg.org/css-flexbox-1/#intrinsic-main-sizes
float FlexFormattingContext::calculate_intrinsic_main_size_of_flex_container()
{
    // The min-content main size of a single-line flex container is calculated identically to the max-content main size,
    // except that the flex items’ min-content contributions are used instead of their max-content contributions.
    // However, for a multi-line container, it is simply the largest min-content contribution of all the non-collapsed flex items in the flex container.
    if (!is_single_line() && m_available_space_for_items->main.is_min_content()) {
        float largest_contribution = 0;
        for (auto const& flex_item : m_flex_items) {
            // FIXME: Skip collapsed flex items.
            largest_contribution = max(largest_contribution, calculate_main_min_content_contribution(flex_item));
        }
        return largest_contribution;
    }

    // The max-content main size of a flex container is, fundamentally, the smallest size the flex container
    // can take such that when flex layout is run with that container size, each flex item ends up at least
    // as large as its max-content contribution, to the extent allowed by the items’ flexibility.
    // It is calculated, considering only non-collapsed flex items, by:

    // 1. For each flex item, subtract its outer flex base size from its max-content contribution size.
    //    If that result is positive, divide it by the item’s flex grow factor if the flex grow factor is ≥ 1,
    //    or multiply it by the flex grow factor if the flex grow factor is < 1; if the result is negative,
    //    divide it by the item’s scaled flex shrink factor (if dividing by zero, treat the result as negative infinity).
    //    This is the item’s desired flex fraction.

    for (auto& flex_item : m_flex_items) {
        float contribution = 0;
        if (m_available_space_for_items->main.is_min_content())
            contribution = calculate_main_min_content_contribution(flex_item);
        else if (m_available_space_for_items->main.is_max_content())
            contribution = calculate_main_max_content_contribution(flex_item);

        float outer_flex_base_size = flex_item.flex_base_size + flex_item.margins.main_before + flex_item.margins.main_after + flex_item.borders.main_before + flex_item.borders.main_after + flex_item.padding.main_before + flex_item.padding.main_after;

        float result = contribution - outer_flex_base_size;
        if (result > 0) {
            if (flex_item.box.computed_values().flex_grow() >= 1) {
                result /= flex_item.box.computed_values().flex_grow();
            } else {
                result *= flex_item.box.computed_values().flex_grow();
            }
        } else if (result < 0) {
            if (flex_item.scaled_flex_shrink_factor == 0)
                result = -INFINITY;
            else
                result /= flex_item.scaled_flex_shrink_factor;
        }

        flex_item.desired_flex_fraction = result;
    }

    // 2. Place all flex items into lines of infinite length.
    m_flex_lines.clear();
    if (!m_flex_items.is_empty())
        m_flex_lines.append(FlexLine {});
    for (auto& flex_item : m_flex_items) {
        // FIXME: Honor breaking requests.
        m_flex_lines.last().items.append(&flex_item);
    }

    //    Within each line, find the greatest (most positive) desired flex fraction among all the flex items.
    //    This is the line’s chosen flex fraction.
    for (auto& flex_line : m_flex_lines) {
        float greatest_desired_flex_fraction = 0;
        float sum_of_flex_grow_factors = 0;
        float sum_of_flex_shrink_factors = 0;
        for (auto& flex_item : flex_line.items) {
            greatest_desired_flex_fraction = max(greatest_desired_flex_fraction, flex_item->desired_flex_fraction);
            sum_of_flex_grow_factors += flex_item->box.computed_values().flex_grow();
            sum_of_flex_shrink_factors += flex_item->box.computed_values().flex_shrink();
        }
        float chosen_flex_fraction = greatest_desired_flex_fraction;

        // 3. If the chosen flex fraction is positive, and the sum of the line’s flex grow factors is less than 1,
        //    divide the chosen flex fraction by that sum.
        if (chosen_flex_fraction > 0 && sum_of_flex_grow_factors < 1)
            chosen_flex_fraction /= sum_of_flex_grow_factors;

        // If the chosen flex fraction is negative, and the sum of the line’s flex shrink factors is less than 1,
        // multiply the chosen flex fraction by that sum.
        if (chosen_flex_fraction < 0 && sum_of_flex_shrink_factors < 1)
            chosen_flex_fraction *= sum_of_flex_shrink_factors;

        flex_line.chosen_flex_fraction = chosen_flex_fraction;
    }

    auto determine_main_size = [&]() -> float {
        float largest_sum = 0;
        for (auto& flex_line : m_flex_lines) {
            // 4. Add each item’s flex base size to the product of its flex grow factor (scaled flex shrink factor, if shrinking)
            //    and the chosen flex fraction, then clamp that result by the max main size floored by the min main size.
            float sum = 0;
            for (auto& flex_item : flex_line.items) {
                float product = 0;
                if (flex_item->desired_flex_fraction > 0)
                    product = flex_line.chosen_flex_fraction * flex_item->box.computed_values().flex_grow();
                else if (flex_item->desired_flex_fraction < 0)
                    product = flex_line.chosen_flex_fraction * flex_item->scaled_flex_shrink_factor;
                auto result = flex_item->flex_base_size + product;

                auto const& computed_min_size = this->computed_main_min_size(flex_item->box);
                auto const& computed_max_size = this->computed_main_max_size(flex_item->box);

                auto clamp_min = (!computed_min_size.is_auto() && !computed_min_size.contains_percentage()) ? specified_main_min_size(flex_item->box) : automatic_minimum_size(*flex_item);
                auto clamp_max = (!computed_max_size.is_none() && !computed_max_size.contains_percentage()) ? specified_main_max_size(flex_item->box) : NumericLimits<float>::max();

                result = css_clamp(result, clamp_min, clamp_max);

                // NOTE: The spec doesn't mention anything about the *outer* size here, but if we don't add the margin box,
                //       flex items with non-zero padding/border/margin in the main axis end up overflowing the container.
                result = flex_item->add_main_margin_box_sizes(result);

                sum += result;
            }
            largest_sum = max(largest_sum, sum);
        }
        // 5. The flex container’s max-content size is the largest sum (among all the lines) of the afore-calculated sizes of all items within a single line.
        return largest_sum;
    };

    auto main_size = determine_main_size();
    set_main_size(flex_container(), main_size);
    return main_size;
}

// https://drafts.csswg.org/css-flexbox-1/#intrinsic-cross-sizes
float FlexFormattingContext::calculate_intrinsic_cross_size_of_flex_container()
{
    // The min-content/max-content cross size of a single-line flex container
    // is the largest min-content contribution/max-content contribution (respectively) of its flex items.
    if (is_single_line()) {
        auto calculate_largest_contribution = [&](bool resolve_percentage_min_max_sizes) {
            float largest_contribution = 0;
            for (auto& flex_item : m_flex_items) {
                float contribution = 0;
                if (m_available_space_for_items->cross.is_min_content())
                    contribution = calculate_cross_min_content_contribution(flex_item, resolve_percentage_min_max_sizes);
                else if (m_available_space_for_items->cross.is_max_content())
                    contribution = calculate_cross_max_content_contribution(flex_item, resolve_percentage_min_max_sizes);
                largest_contribution = max(largest_contribution, contribution);
            }
            return largest_contribution;
        };

        auto first_pass_largest_contribution = calculate_largest_contribution(false);
        set_cross_size(flex_container(), first_pass_largest_contribution);
        auto second_pass_largest_contribution = calculate_largest_contribution(true);
        return second_pass_largest_contribution;
    }

    if (is_row_layout()) {
        // row multi-line flex container cross-size

        // The min-content/max-content cross size is the sum of the flex line cross sizes resulting from
        // sizing the flex container under a cross-axis min-content constraint/max-content constraint (respectively).

        // NOTE: We fall through to the ad-hoc section below.
    } else {
        // column multi-line flex container cross-size

        // The min-content cross size is the largest min-content contribution among all of its flex items.
        if (m_available_space_for_items->cross.is_min_content()) {
            auto calculate_largest_contribution = [&](bool resolve_percentage_min_max_sizes) {
                float largest_contribution = 0;
                for (auto& flex_item : m_flex_items) {
                    float contribution = calculate_cross_min_content_contribution(flex_item, resolve_percentage_min_max_sizes);
                    largest_contribution = max(largest_contribution, contribution);
                }
                return largest_contribution;
            };

            auto first_pass_largest_contribution = calculate_largest_contribution(false);
            set_cross_size(flex_container(), first_pass_largest_contribution);
            auto second_pass_largest_contribution = calculate_largest_contribution(true);
            return second_pass_largest_contribution;
        }

        // The max-content cross size is the sum of the flex line cross sizes resulting from
        // sizing the flex container under a cross-axis max-content constraint,
        // using the largest max-content cross-size contribution among the flex items
        // as the available space in the cross axis for each of the flex items during layout.

        // NOTE: We fall through to the ad-hoc section below.
    }

    // HACK: We run steps 5, 7, 9 and 11 from the main algorithm. This gives us *some* cross size information to work with.
    m_flex_lines.clear();
    collect_flex_items_into_flex_lines();

    for (auto& flex_item : m_flex_items) {
        determine_hypothetical_cross_size_of_item(flex_item, false);
    }
    calculate_cross_size_of_each_flex_line();
    determine_used_cross_size_of_each_flex_item();

    float sum_of_flex_line_cross_sizes = 0;
    for (auto& flex_line : m_flex_lines) {
        sum_of_flex_line_cross_sizes += flex_line.cross_size;
    }
    return sum_of_flex_line_cross_sizes;
}

// https://drafts.csswg.org/css-flexbox-1/#intrinsic-item-contributions
float FlexFormattingContext::calculate_main_min_content_contribution(FlexItem const& item) const
{
    // The main-size min-content contribution of a flex item is
    // the larger of its outer min-content size and outer preferred size if that is not auto,
    // clamped by its min/max main size.
    auto larger_size = [&] {
        auto inner_min_content_size = calculate_min_content_main_size(item);
        if (computed_main_size(item.box).is_auto())
            return inner_min_content_size;
        auto inner_preferred_size = is_row_layout() ? get_pixel_width(item.box, computed_main_size(item.box)) : get_pixel_height(item.box, computed_main_size(item.box));
        return max(inner_min_content_size, inner_preferred_size);
    }();

    auto clamp_min = has_main_min_size(item.box) ? specified_main_min_size(item.box) : automatic_minimum_size(item);
    auto clamp_max = has_main_max_size(item.box) ? specified_main_max_size(item.box) : NumericLimits<float>::max();
    auto clamped_inner_size = css_clamp(larger_size, clamp_min, clamp_max);

    return item.add_main_margin_box_sizes(clamped_inner_size);
}

// https://drafts.csswg.org/css-flexbox-1/#intrinsic-item-contributions
float FlexFormattingContext::calculate_main_max_content_contribution(FlexItem const& item) const
{
    // The main-size max-content contribution of a flex item is
    // the larger of its outer max-content size and outer preferred size if that is not auto,
    // clamped by its min/max main size.
    auto larger_size = [&] {
        auto inner_max_content_size = calculate_max_content_main_size(item);
        if (computed_main_size(item.box).is_auto())
            return inner_max_content_size;
        auto inner_preferred_size = is_row_layout() ? get_pixel_width(item.box, computed_main_size(item.box)) : get_pixel_height(item.box, computed_main_size(item.box));
        return max(inner_max_content_size, inner_preferred_size);
    }();

    auto clamp_min = has_main_min_size(item.box) ? specified_main_min_size(item.box) : automatic_minimum_size(item);
    auto clamp_max = has_main_max_size(item.box) ? specified_main_max_size(item.box) : NumericLimits<float>::max();
    auto clamped_inner_size = css_clamp(larger_size, clamp_min, clamp_max);

    return item.add_main_margin_box_sizes(clamped_inner_size);
}

bool FlexFormattingContext::should_treat_main_size_as_auto(Box const& box) const
{
    if (is_row_layout())
        return should_treat_width_as_auto(box, m_available_space_for_items->space);
    return should_treat_height_as_auto(box, m_available_space_for_items->space);
}

bool FlexFormattingContext::should_treat_cross_size_as_auto(Box const& box) const
{
    if (is_row_layout())
        return should_treat_height_as_auto(box, m_available_space_for_items->space);
    return should_treat_width_as_auto(box, m_available_space_for_items->space);
}

float FlexFormattingContext::calculate_cross_min_content_contribution(FlexItem const& item, bool resolve_percentage_min_max_sizes) const
{
    auto size = [&] {
        if (should_treat_cross_size_as_auto(item.box))
            return calculate_min_content_cross_size(item);
        return !is_row_layout() ? get_pixel_width(item.box, computed_cross_size(item.box)) : get_pixel_height(item.box, computed_cross_size(item.box));
    }();

    auto const& computed_min_size = this->computed_cross_min_size(item.box);
    auto const& computed_max_size = this->computed_cross_max_size(item.box);

    auto clamp_min = (!computed_min_size.is_auto() && (resolve_percentage_min_max_sizes || !computed_min_size.contains_percentage())) ? specified_cross_min_size(item.box) : 0;
    auto clamp_max = (!computed_max_size.is_none() && (resolve_percentage_min_max_sizes || !computed_max_size.contains_percentage())) ? specified_cross_max_size(item.box) : NumericLimits<float>::max();

    auto clamped_inner_size = css_clamp(size, clamp_min, clamp_max);

    return item.add_cross_margin_box_sizes(clamped_inner_size);
}

float FlexFormattingContext::calculate_cross_max_content_contribution(FlexItem const& item, bool resolve_percentage_min_max_sizes) const
{
    auto size = [&] {
        if (should_treat_cross_size_as_auto(item.box))
            return calculate_max_content_cross_size(item);
        return !is_row_layout() ? get_pixel_width(item.box, computed_cross_size(item.box)) : get_pixel_height(item.box, computed_cross_size(item.box));
    }();

    auto const& computed_min_size = this->computed_cross_min_size(item.box);
    auto const& computed_max_size = this->computed_cross_max_size(item.box);

    auto clamp_min = (!computed_min_size.is_auto() && (resolve_percentage_min_max_sizes || !computed_min_size.contains_percentage())) ? specified_cross_min_size(item.box) : 0;
    auto clamp_max = (!computed_max_size.is_none() && (resolve_percentage_min_max_sizes || !computed_max_size.contains_percentage())) ? specified_cross_max_size(item.box) : NumericLimits<float>::max();

    auto clamped_inner_size = css_clamp(size, clamp_min, clamp_max);

    return item.add_cross_margin_box_sizes(clamped_inner_size);
}

float FlexFormattingContext::calculate_min_content_main_size(FlexItem const& item) const
{
    return is_row_layout() ? calculate_min_content_width(item.box) : calculate_min_content_height(item.box, m_available_space_for_items->space.width);
}

float FlexFormattingContext::calculate_fit_content_main_size(FlexItem const& item) const
{
    return is_row_layout() ? calculate_fit_content_width(item.box, m_available_space_for_items->space)
                           : calculate_fit_content_height(item.box, m_available_space_for_items->space);
}

float FlexFormattingContext::calculate_fit_content_cross_size(FlexItem const& item) const
{
    return !is_row_layout() ? calculate_fit_content_width(item.box, m_available_space_for_items->space)
                            : calculate_fit_content_height(item.box, m_available_space_for_items->space);
}

float FlexFormattingContext::calculate_max_content_main_size(FlexItem const& item) const
{
    return is_row_layout() ? calculate_max_content_width(item.box) : calculate_max_content_height(item.box, m_available_space_for_items->space.width);
}

float FlexFormattingContext::calculate_min_content_cross_size(FlexItem const& item) const
{
    return is_row_layout() ? calculate_min_content_height(item.box, m_available_space_for_items->space.width) : calculate_min_content_width(item.box);
}

float FlexFormattingContext::calculate_max_content_cross_size(FlexItem const& item) const
{
    return is_row_layout() ? calculate_max_content_height(item.box, m_available_space_for_items->space.width) : calculate_max_content_width(item.box);
}

// https://drafts.csswg.org/css-flexbox-1/#stretched
bool FlexFormattingContext::flex_item_is_stretched(FlexItem const& item) const
{
    auto alignment = alignment_for_item(item.box);
    if (alignment != CSS::AlignItems::Stretch)
        return false;
    // If the cross size property of the flex item computes to auto, and neither of the cross-axis margins are auto, the flex item is stretched.
    auto const& computed_cross_size = is_row_layout() ? item.box.computed_values().height() : item.box.computed_values().width();
    return computed_cross_size.is_auto() && !item.margins.cross_before_is_auto && !item.margins.cross_after_is_auto;
}

CSS::Size const& FlexFormattingContext::computed_main_size(Box const& box) const
{
    return is_row_layout() ? box.computed_values().width() : box.computed_values().height();
}

CSS::Size const& FlexFormattingContext::computed_main_min_size(Box const& box) const
{
    return is_row_layout() ? box.computed_values().min_width() : box.computed_values().min_height();
}

CSS::Size const& FlexFormattingContext::computed_main_max_size(Box const& box) const
{
    return is_row_layout() ? box.computed_values().max_width() : box.computed_values().max_height();
}

CSS::Size const& FlexFormattingContext::computed_cross_size(Box const& box) const
{
    return !is_row_layout() ? box.computed_values().width() : box.computed_values().height();
}

CSS::Size const& FlexFormattingContext::computed_cross_min_size(Box const& box) const
{
    return !is_row_layout() ? box.computed_values().min_width() : box.computed_values().min_height();
}

CSS::Size const& FlexFormattingContext::computed_cross_max_size(Box const& box) const
{
    return !is_row_layout() ? box.computed_values().max_width() : box.computed_values().max_height();
}

// https://drafts.csswg.org/css-flexbox-1/#algo-cross-margins
void FlexFormattingContext::resolve_cross_axis_auto_margins()
{
    for (auto& line : m_flex_lines) {
        for (auto& item : line.items) {
            //  If a flex item has auto cross-axis margins:
            if (!item->margins.cross_before_is_auto && !item->margins.cross_after_is_auto)
                continue;

            // If its outer cross size (treating those auto margins as zero) is less than the cross size of its flex line,
            // distribute the difference in those sizes equally to the auto margins.
            auto outer_cross_size = item->cross_size + item->padding.cross_before + item->padding.cross_after + item->borders.cross_before + item->borders.cross_after;
            if (outer_cross_size < line.cross_size) {
                float remainder = line.cross_size - outer_cross_size;
                if (item->margins.cross_before_is_auto && item->margins.cross_after_is_auto) {
                    item->margins.cross_before = remainder / 2.0f;
                    item->margins.cross_after = remainder / 2.0f;
                } else if (item->margins.cross_before_is_auto) {
                    item->margins.cross_before = remainder;
                } else {
                    item->margins.cross_after = remainder;
                }
            } else {
                // FIXME: Otherwise, if the block-start or inline-start margin (whichever is in the cross axis) is auto, set it to zero.
                //        Set the opposite margin so that the outer cross size of the item equals the cross size of its flex line.
            }
        }
    }
}

// https://drafts.csswg.org/css-flexbox-1/#algo-line-stretch
void FlexFormattingContext::handle_align_content_stretch()
{
    // If the flex container has a definite cross size,
    if (!has_definite_cross_size(flex_container()))
        return;

    // align-content is stretch,
    if (flex_container().computed_values().align_content() != CSS::AlignContent::Stretch)
        return;

    // and the sum of the flex lines' cross sizes is less than the flex container’s inner cross size,
    float sum_of_flex_line_cross_sizes = 0;
    for (auto& line : m_flex_lines)
        sum_of_flex_line_cross_sizes += line.cross_size;

    if (sum_of_flex_line_cross_sizes >= specified_cross_size(flex_container()))
        return;

    // increase the cross size of each flex line by equal amounts
    // such that the sum of their cross sizes exactly equals the flex container’s inner cross size.
    float remainder = specified_cross_size(flex_container()) - sum_of_flex_line_cross_sizes;
    float extra_per_line = remainder / m_flex_lines.size();

    for (auto& line : m_flex_lines)
        line.cross_size += extra_per_line;
}

// https://drafts.csswg.org/css-flexbox-1/#abspos-items
Gfx::FloatPoint FlexFormattingContext::calculate_static_position(Box const& box) const
{
    // The cross-axis edges of the static-position rectangle of an absolutely-positioned child
    // of a flex container are the content edges of the flex container.
    float cross_offset = 0;
    float half_line_size = specified_cross_size(flex_container()) / 2;

    auto const& box_state = m_state.get(box);
    float cross_margin_before = is_row_layout() ? box_state.margin_top : box_state.margin_left;
    float cross_margin_after = is_row_layout() ? box_state.margin_bottom : box_state.margin_right;
    float cross_border_before = is_row_layout() ? box_state.border_top : box_state.border_left;
    float cross_border_after = is_row_layout() ? box_state.border_bottom : box_state.border_right;
    float cross_padding_before = is_row_layout() ? box_state.padding_top : box_state.padding_left;
    float cross_padding_after = is_row_layout() ? box_state.padding_bottom : box_state.padding_right;

    switch (alignment_for_item(box)) {
    case CSS::AlignItems::Baseline:
        // FIXME: Implement this
        //  Fallthrough
    case CSS::AlignItems::FlexStart:
    case CSS::AlignItems::Stretch:
        cross_offset = 0 - half_line_size + cross_margin_before + cross_border_before + cross_padding_before;
        break;
    case CSS::AlignItems::FlexEnd:
        cross_offset = half_line_size - specified_cross_size(box) - cross_margin_after - cross_border_after - cross_padding_after;
        break;
    case CSS::AlignItems::Center:
        cross_offset = 0 - (specified_cross_size(box) / 2.0f);
        break;
    default:
        break;
    }

    cross_offset += specified_cross_size(flex_container()) / 2.0f;

    // The main-axis edges of the static-position rectangle are where the margin edges of the child
    // would be positioned if it were the sole flex item in the flex container,
    // assuming both the child and the flex container were fixed-size boxes of their used size.
    // (For this purpose, auto margins are treated as zero.

    bool pack_from_end = true;
    float main_offset = 0;
    switch (flex_container().computed_values().justify_content()) {
    case CSS::JustifyContent::FlexStart:
        if (is_direction_reverse()) {
            pack_from_end = false;
            main_offset = specified_main_size(flex_container());
        } else {
            main_offset = 0;
        }
        break;
    case CSS::JustifyContent::FlexEnd:
        if (is_direction_reverse()) {
            main_offset = 0;
        } else {
            pack_from_end = false;
            main_offset = specified_main_size(flex_container());
        }
        break;
    case CSS::JustifyContent::SpaceBetween:
        main_offset = 0;
        break;
    case CSS::JustifyContent::Center:
    case CSS::JustifyContent::SpaceAround:
        main_offset = specified_main_size(flex_container()) / 2.0f - specified_main_size(box) / 2.0f;
        break;
    }

    // NOTE: Next, we add the flex container's padding since abspos boxes are placed relative to the padding edge
    //       of their abspos containing block.
    if (pack_from_end) {
        main_offset += is_row_layout() ? m_flex_container_state.padding_left : m_flex_container_state.padding_top;
    } else {
        main_offset += is_row_layout() ? m_flex_container_state.padding_right : m_flex_container_state.padding_bottom;
    }

    if (!pack_from_end)
        main_offset += specified_main_size(flex_container()) - specified_main_size(box);

    auto static_position_offset = is_row_layout() ? Gfx::FloatPoint { main_offset, cross_offset } : Gfx::FloatPoint { cross_offset, main_offset };

    auto absolute_position_of_flex_container = absolute_content_rect(flex_container(), m_state).location();
    auto absolute_position_of_abspos_containing_block = absolute_content_rect(*box.containing_block(), m_state).location();
    auto diff = absolute_position_of_flex_container - absolute_position_of_abspos_containing_block;

    return static_position_offset + diff;
}

}
